package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.app.port.BranchOperationHoursPort;
import capitec.branch.appointment.branch.app.port.OperationHourDetails;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfoService;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.sharekernel.day.app.GetDateOfNextDaysQuery;
import capitec.branch.appointment.utils.sharekernel.day.domain.Day;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.Comparator;
import java.util.List;

import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class AddBranchAppointmentInfoUseCase {

    private static final String COUNTRY = "South Africa";
    private final BranchService branchService;
    private final BranchAppointmentInfoService branchAppointmentInfoService;
    private final BranchOperationHoursPort branchOperationHoursPort;
    private final GetDateOfNextDaysQuery getDateOfNextDaysQuery;

    public boolean execute(String branchId, @Valid BranchAppointmentInfoDTO dto) {

        return executeWithExceptionHandling(dto, () -> {
            Branch branch = getByBranchIdOrThrow(branchId);

            BranchAppointmentInfo info = new BranchAppointmentInfo(
                    dto.slotDuration(),
                    dto.utilizationFactor(),
                    dto.staffCount(),
                    dto.day(),
                    dto.maxBookingCapacity()
            );


            LocalTime openAt ;
            LocalTime closeAt;

            List<OperationHoursOverride> operationHoursOverride = branch.getOperationHoursOverride();

            if(operationHoursOverride != null && !operationHoursOverride.isEmpty()){
                var hoursOverride = operationHoursOverride
                                    .stream()
                                    .filter(op -> dto.day().equals(op.effectiveDate()))
                                    .findFirst();

                if(hoursOverride.isPresent()){

                    openAt=hoursOverride.get().openAt();
                    closeAt=hoursOverride.get().closeAt();
                }
                else{
                    // no overrides for the give day, use branch locator service
                    var operationHourDetails = getOperationHoursOrThrow(branchId, dto);
                    validateBranchIsOpen(operationHourDetails);

                    openAt = operationHourDetails.openTime();
                    closeAt = operationHourDetails.closingTime();
                }
            }
            else {
                // no overrides at all, use branch locator service
                var operationHourDetails = getOperationHoursOrThrow(branchId, dto);
                validateBranchIsOpen(operationHourDetails);

                openAt = operationHourDetails.openTime();
                closeAt = operationHourDetails.closingTime();
            }


            branch.updateAppointmentInfo(dto.day(), info,openAt, closeAt);

            return branchAppointmentInfoService.addBranchAppointmentConfigInfo(dto.day(), branch);
        });
    }

    private Branch getByBranchIdOrThrow(String branchId) {
        return branchService.getByBranchId(branchId)
                .orElseThrow(() -> {
                    log.error("Unable to find branch with id {}", branchId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
                });
    }

    private OperationHourDetails getOperationHoursOrThrow(String branchId, BranchAppointmentInfoDTO dto) {
        var isHoliday  = switch (dto.day()){
            case PUBLIC_HOLIDAY -> true;
            case MONDAY, TUESDAY ,WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY -> false;
        };

        if (isHoliday) {
            LocalDate now = LocalDate.now();
            LocalDate lastDayOfYear = now.withDayOfYear(now.lengthOfYear());

            // Get all holidays dates from now to the last day of the year
            List<Day> holidays = getDateOfNextDaysQuery.execute(now, lastDayOfYear)
                    .stream()
                    .filter(Day::isHoliday)
                    .toList();

            // Get operation hours on holidays and find the one with the shortest duration
            // to ensure slot config fits all holiday schedules
            return holidays.stream()
                    .map(day -> branchOperationHoursPort.getOperationHours(COUNTRY, branchId, day.getDate()).orElse(null))
                    .filter(Objects::nonNull)
                    .min(Comparator.comparing(op -> Duration.between(op.openTime(), op.closingTime())))
                    .orElseThrow(() -> {
                        log.warn("Branch is closed on public holiday, " +
                                "override branch operation hours for the day. branchId:{} day:{}", branchId, dto.day());
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "No operation hours found for the day, branch operation hours for the day");
                    });
        }
        else {
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(dto.day().name());
            LocalDate now = LocalDate.now();

            // Find the next occurrence of the day of the week
            LocalDate targetDate = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(dayOfWeek));

            return branchOperationHoursPort.getOperationHours(COUNTRY, branchId, targetDate)
                    .orElseThrow(() -> {
                        log.warn("No operation hours found for the day {}", dto.day());
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "No operation hours found for the day");
                    });
        }
    }

    private void validateBranchIsOpen(OperationHourDetails operationHourDetails) {
        if (operationHourDetails.closed()) {
            log.warn("Operation hours for the day are closed, details: {}", operationHourDetails);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operation hours for the day are closed");
        }
    }

    private Boolean executeWithExceptionHandling(BranchAppointmentInfoDTO dto, Supplier<Boolean> action) {
        try {
            return action.get();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid arguments for the branch appointment info, input dto:{}",dto, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        catch (Exception e) {
            log.error("Internal server error.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.", e);
        }
    }
}
