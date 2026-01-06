package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.staff.domain.StaffStatus;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;


@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
class UpdateStaffWorkStatusUseCase {

    private final StaffService staffService;

    public Staff execute(String username, StaffStatus status) {

        Optional<Staff> isUpdated;
        try {

            isUpdated = staffService.updateStaffWorkStatus(username, status);

        } catch (Exception e) {
            log.error("Error updating staff {}", username, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }

        return isUpdated.orElseThrow(() -> {
            log.error("Staff with username:{} is not found ", username);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found.");
        });
    }
}