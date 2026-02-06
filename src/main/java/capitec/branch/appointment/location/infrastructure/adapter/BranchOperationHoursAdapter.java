package capitec.branch.appointment.location.infrastructure.adapter;

import capitec.branch.appointment.branch.app.port.BranchDetails;
import capitec.branch.appointment.branch.app.port.BranchOperationHoursPort;
import capitec.branch.appointment.branch.app.port.OperationHourDetails;
import capitec.branch.appointment.location.app.NearbyBranchDTO;
import capitec.branch.appointment.location.app.OperationTimeDTO;
import capitec.branch.appointment.location.app.SearchBranchesByAreaQuery;
import capitec.branch.appointment.location.app.SearchBranchesByAreaUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchOperationHoursAdapter implements BranchOperationHoursPort {

    private final SearchBranchesByAreaUseCase searchBranchesByAreaUseCase;


    @Override
    public Optional<OperationHourDetails> getOperationHours(String country, String branchId, LocalDate day) {

        List<NearbyBranchDTO> branchDTOList = searchBranchesByAreaUseCase.execute(new SearchBranchesByAreaQuery(country));

        for (NearbyBranchDTO nearbyBranchDTO : branchDTOList) {

            Map<LocalDate, OperationTimeDTO> operationTimeMap = nearbyBranchDTO.operationTimes();
            OperationTimeDTO operationTime = operationTimeMap.get(day);

            if( nearbyBranchDTO.branchId().equals(branchId) && operationTime !=null){
                return Optional.of(new OperationHourDetails(
                        operationTime.openAt(), operationTime.closeAt(),
                        operationTime.closed()));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean checkExist(String country, String branchId) {
       return searchBranchesByAreaUseCase.execute(new SearchBranchesByAreaQuery(country))
                .stream()
               .map(NearbyBranchDTO::branchId)
                .anyMatch(branchId::equals);
    }

    @Override
    public Optional<BranchDetails> getBranchNames(String country, String branchId) {
       return searchBranchesByAreaUseCase.execute(new SearchBranchesByAreaQuery(country))
                .stream()
                .filter(b-> b.branchId().equals(branchId))
                .map(b->new BranchDetails(b.name(),b.branchCode()))
                .findFirst();
    }
}
