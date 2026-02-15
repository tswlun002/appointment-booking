package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignmentService;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class CancelFutureWorkingDaysUseCase {

    private final BranchStaffAssignmentService branchStaffAssignmentService;
    public void execute(String branchId, @NotEmpty @NotNull DayOfWeek... days) {

        LocalDate today = LocalDate.now();
        Set<LocalDate> datesToCancel = new HashSet<>();

        // Logic to find all future dates matching the DayOfWeek input
        for (int i = 0; i < 7; i++) { // Check the next 7 days, or adjust range as needed
            LocalDate date = today.plusDays(i);

            boolean match = Arrays.asList(days).contains(date.getDayOfWeek());

            if (match) {
                datesToCancel.add(date);
            }
        }
        
        // We only proceed if dates were actually found
        if (datesToCancel.isEmpty()) {
            log.warn("No future dates matching the cancellation request were found for branch {}", branchId);
            return; 
        }

        try {
            branchStaffAssignmentService.cancelWorkingDay(branchId, datesToCancel);
        } catch (Exception e) {
            log.error("Failed to cancel working days {} of branch {}", datesToCancel, branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",e);
        }
    }
}