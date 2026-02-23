package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.app.port.BranchOperationHoursPort;
import capitec.branch.appointment.branch.app.port.BranchQueryPort;
import capitec.branch.appointment.branch.app.port.OperationHourDetails;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfoService;
import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.sharekernel.day.app.GetDateOfNextDaysQuery;
import capitec.branch.appointment.sharekernel.day.domain.Day;
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

/**
 * Use case for configuring appointment slot settings for a specific branch and day type.
 *
 * <p>This configuration is used by the {@code SlotGeneratorScheduler} to create bookable
 * appointment slots. Each branch can have different slot configurations for different day types
 * (Monday-Sunday, Public Holidays).</p>
 *
 * <h2>Input ({@link BranchAppointmentInfoDTO}):</h2>
 * <ul>
 *   <li><b>staffCount</b> - Number of staff available for appointments on that day</li>
 *   <li><b>slotDuration</b> - Duration of each appointment slot (e.g., PT30M for 30 minutes)</li>
 *   <li><b>utilizationFactor</b> - Efficiency factor (e.g., 0.8 means 80% of time is bookable)</li>
 *   <li><b>day</b> - Day type: MONDAY, TUESDAY, ..., SUNDAY, or PUBLIC_HOLIDAY</li>
 *   <li><b>maxBookingCapacity</b> - Maximum number of appointments per slot</li>
 * </ul>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Fetches the branch from database (throws 404 if not found)</li>
 *   <li>Creates a {@link BranchAppointmentInfo} domain object with slot configuration</li>
 *   <li>Determines operation hours (openAt, closeAt) using the following priority:
 *     <ul>
 *       <li>First, checks for {@link OperationHoursOverride} matching the day type</li>
 *       <li>If no override found, fetches from Branch Locator API</li>
 *       <li>For PUBLIC_HOLIDAY: gets all remaining holidays this year and uses the shortest
 *           operating window to ensure slot config works for all holidays</li>
 *     </ul>
 *   </li>
 *   <li>Validates the branch is open on that day</li>
 *   <li>Updates the branch with the appointment info and persists to database</li>
 * </ol>
 *
 * <h2>Business Rules:</h2>
 * <ul>
 *   <li>Branch must exist</li>
 *   <li>Operation hours must exist (either from override or Branch Locator API)</li>
 *   <li>Branch must be open on that day (cannot configure slots for closed days)</li>
 * </ul>
 *
 * <h2>Example Use Case:</h2>
 * <p>Admin configures Monday appointments for branch 470010:</p>
 * <pre>
 * {
 *   "staffCount": 3,
 *   "slotDuration": "PT30M",
 *   "utilizationFactor": 0.8,
 *   "day": "MONDAY",
 *   "maxBookingCapacity": 5
 * }
 * </pre>
 * <p>The system will:</p>
 * <ol>
 *   <li>Find branch 470010</li>
 *   <li>Check if there's an override for any Monday → No</li>
 *   <li>Get operation hours from Branch Locator API for next Monday → Opens 8:00, Closes 17:00</li>
 *   <li>Save: "On Mondays, branch 470010 has 30-min slots, 3 staff, max 5 bookings per slot"</li>
 * </ol>
 *
 * @see BranchAppointmentInfo
 * @see BranchAppointmentInfoDTO
 * @see OperationHoursOverride
 * @see BranchAppointmentInfoService
 */
@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class AddBranchAppointmentInfoUseCase {

    private static final String COUNTRY = "South Africa";
    private final BranchQueryPort branchQueryPort;
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
                //check if there override hours future day, we use it
                var hoursOverride = operationHoursOverride
                                    .stream()
                                    .filter(op ->{
                                         if(dto.day()== DayType.PUBLIC_HOLIDAY && !getDateOfNextDaysQuery.execute(op.effectiveDate()).isHoliday()){
                                             return false;
                                         }
                                         else if (dto.day()== DayType.PUBLIC_HOLIDAY && getDateOfNextDaysQuery.execute(op.effectiveDate()).isHoliday()){
                                             return true;
                                         }
                                        return dto.day().name().equals(op.effectiveDate().getDayOfWeek().name());
                                    })
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
        return branchQueryPort.findByBranchId(branchId)
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
                                "No operation hours found for the day. Please add branch operation hours for the day first.");
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
