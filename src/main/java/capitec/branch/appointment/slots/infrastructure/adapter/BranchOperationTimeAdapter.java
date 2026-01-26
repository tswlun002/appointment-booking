package capitec.branch.appointment.slots.infrastructure.adapter;

import capitec.branch.appointment.branch.app.GetBranchQuery;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import capitec.branch.appointment.location.app.NearbyBranchDTO;
import capitec.branch.appointment.location.app.SearchBranchesByAreaQuery;
import capitec.branch.appointment.location.app.SearchBranchesByAreaUseCase;
import capitec.branch.appointment.slots.app.port.AppointmentInfoDetails;
import capitec.branch.appointment.slots.app.port.BranchOperationTimesDetails;
import capitec.branch.appointment.slots.app.port.GetActiveBranchesForSlotGenerationPort;
import capitec.branch.appointment.slots.app.port.OperationTimesDetails;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j


public class BranchOperationTimeAdapter implements GetActiveBranchesForSlotGenerationPort {

    private final SearchBranchesByAreaUseCase searchBranchesByAreaUseCase;
    private final GetBranchQuery getBranchQuery;
    private final AsyncTaskExecutor taskExecutor;

    Set<LocalDate> nextSevenDays = Stream.iterate(LocalDate.now(), d -> d.plusDays(1))
            .limit(7)
            .collect(Collectors.toSet());

    public BranchOperationTimeAdapter(SearchBranchesByAreaUseCase searchBranchesByAreaUseCase, GetBranchQuery getBranchQuery,
                                      @Qualifier("applicationTaskExecutor") AsyncTaskExecutor taskExecutor) {
        this.searchBranchesByAreaUseCase = searchBranchesByAreaUseCase;
        this.getBranchQuery = getBranchQuery;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public Collection<BranchOperationTimesDetails> execute(String country, LocalDate fromDate) {
        return getBranchesAggregated(country,fromDate, Duration.ofSeconds(5), Duration.ofSeconds(10));
    }

    /**
     * Aggregates branches. If any source fails, the exception is thrown to the caller.
     */
    public Collection<BranchOperationTimesDetails> getBranchesAggregated(String country, LocalDate fromDate, Duration timeoutDb, Duration timeoutApi) {
        log.info("Starting aggregation for {}. Fail-fast mode enabled.", country);
        Instant start = Instant.now();

        // 1. Define Suppliers
        Supplier<List<Branch>> dbFetch = () -> getBranchQuery.execute(0, 1000)
                .stream()
                //.map(this::mapDbToDto)
                .toList();

        Supplier<List<NearbyBranchDTO>> apiFetch = () -> {
            SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery(country);
            return searchBranchesByAreaUseCase.execute(query)
                    .stream()
                    .toList();
        };

        // 2. Fork Tasks (Using Virtual Threads via taskExecutor)
        var dbFuture = CompletableFuture.supplyAsync(dbFetch, taskExecutor)
                .orTimeout(timeoutDb.toMillis(), TimeUnit.MILLISECONDS);

        var apiFuture = CompletableFuture.supplyAsync(apiFetch, taskExecutor)
                .orTimeout(timeoutApi.toMillis(), TimeUnit.MILLISECONDS);

        // 3. Join and Merge
        // Note: .join() will throw CompletionException if either task fails or times out.
        Collection<BranchOperationTimesDetails> results = CompletableFuture.allOf(dbFuture, apiFuture)
                .thenApply(_ -> mergeAndDeduplicate(fromDate,dbFuture.join(), apiFuture.join()))
                .join();

        log.info("Aggregated {} branches in {}ms", results.size(), Duration.between(start, Instant.now()).toMillis());
        return results;
    }

    private Collection<BranchOperationTimesDetails> mergeAndDeduplicate(
            LocalDate fromDate,
            List<Branch> dbResult,
            List<NearbyBranchDTO> apiResult) {

        // 1. Index DB results by ID for O(1) fast lookup
        Map<String, Branch> dbBrainchIdMap = dbResult.stream()
                .collect(Collectors.toMap(Branch::getBranchId, b -> b));

        Map<String, NearbyBranchDTO> apiBranchIdMap = apiResult.stream()
                .collect(Collectors.toMap(NearbyBranchDTO::branchId, b -> b));



        // 2. We only care about branches that exist in BOTH (Registered in our system)
        // and we use the API data as the foundational structure.
        return apiBranchIdMap.keySet().stream()
                .filter(dbBrainchIdMap::containsKey)
                .map(branchId -> mapToBranchOperationTimesDto(fromDate,branchId,dbBrainchIdMap,apiBranchIdMap))
                .toList();
    }

    private BranchOperationTimesDetails mapToBranchOperationTimesDto(
            LocalDate fromDate ,String branchId,
      Map<String, Branch> dbBrainchIdMap,
      Map<String, NearbyBranchDTO> apiBranchIdMap     ){

        Branch dbBranch = dbBrainchIdMap.get(branchId);
        NearbyBranchDTO apiBranch = apiBranchIdMap.get(branchId);


        Map<LocalDate, AppointmentInfoDetails> finalAppointmentMap = dbBranch.getBranchAppointmentInfo()
                .stream()
                .filter(b->
                        nextSevenDays.stream().anyMatch(date -> date.equals(b.day()))
                )
                .collect(Collectors.toMap(
                        BranchAppointmentInfo::day,
                        this::mapToAppointmentInfo
                ));

        Map<LocalDate, OperationTimesDetails> finalOperationMap = new HashMap<>();

        // 1. Always take Appointment Info from DB
        var operationHoursOverride = dbBranch.getOperationHoursOverride();

        // Get override time of the future dates
        // Index the override by their effective date

        Map<LocalDate,OperationHoursOverride> mapOverrideOperationTime = operationHoursOverride != null? operationHoursOverride.stream()
                .filter(override -> fromDate.isBefore(override.effectiveDate()) ||
                        fromDate.isEqual(override.effectiveDate())
                )
                .collect(Collectors.toMap(OperationHoursOverride::effectiveDate, override -> override))
                : Collections.emptyMap();

        var apiOperationTimesDtoMap = apiBranch.operationTimes();

        if(apiOperationTimesDtoMap != null) {

            // Loop through days provided by the Locator API
            for (var operationTimeFromApiKey : apiOperationTimesDtoMap.keySet()) {

                // 2. Operation Times: Prioritize DB Override, fallback to API
                var overrideOfTheDay = mapOverrideOperationTime.get(operationTimeFromApiKey);
                if (overrideOfTheDay != null) {

                    var timesDto = new OperationTimesDetails(overrideOfTheDay.openAt(),
                            overrideOfTheDay.closeAt(), overrideOfTheDay.closed());
                    finalOperationMap.put(operationTimeFromApiKey, timesDto);

                } else {
                    // fallback to API operation times
                    var operationTimesDto = apiOperationTimesDtoMap.get(operationTimeFromApiKey);
                    var timesDto = new OperationTimesDetails(operationTimesDto.openAt(),operationTimesDto.closeAt(),
                            operationTimesDto.closed());

                    finalOperationMap.put(operationTimeFromApiKey, timesDto);
                }
            }
        }
        else {
            log.warn("No operation time for branch {} found", branchId);
        }

        return new BranchOperationTimesDetails(branchId, finalOperationMap, finalAppointmentMap);
    }

    private AppointmentInfoDetails mapToAppointmentInfo(BranchAppointmentInfo info) {
        return new AppointmentInfoDetails(
                info.slotDuration(),info.staffCount(),info.utilizationFactor(),
                info.maxBookingCapacity()
        );
    }

}