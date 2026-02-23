package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.staff.domain.StaffStatus;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

/**
 * Use case for adding a new staff member to a branch.
 *
 * <p>This use case handles the registration of staff members who will serve customers
 * at a specific branch. New staff are automatically assigned TRAINING status until
 * they are activated by an administrator.</p>
 *
 * <h2>Input ({@link StaffDTO}):</h2>
 * <ul>
 *   <li><b>username</b> - The username of the user to be added as staff (must exist in the system)</li>
 *   <li><b>branchId</b> - The branch where the staff member will work</li>
 * </ul>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Validates that the user exists in the company system (Keycloak)</li>
 *   <li>Creates a new {@link Staff} domain object with TRAINING status</li>
 *   <li>Persists the staff member to the database</li>
 * </ol>
 *
 * <h2>Business Rules:</h2>
 * <ul>
 *   <li>User must exist in the company system before being added as staff</li>
 *   <li>Staff cannot be added to the same branch twice (unique constraint)</li>
 *   <li>New staff start with TRAINING status</li>
 * </ul>
 *
 * <h2>Error Handling:</h2>
 * <ul>
 *   <li><b>NOT_FOUND (404)</b> - User doesn't exist in the company system</li>
 *   <li><b>CONFLICT (409)</b> - Staff already exists in this branch</li>
 *   <li><b>INTERNAL_SERVER_ERROR (500)</b> - Unexpected errors</li>
 * </ul>
 *
 * <h2>Example Use Case:</h2>
 * <p>Admin adds a new staff member to branch 470010:</p>
 * <pre>
 * {
 *   "username": "john.doe",
 *   "branchId": "470010"
 * }
 * </pre>
 * <p>The system will:</p>
 * <ol>
 *   <li>Verify john.doe exists in Keycloak</li>
 *   <li>Create staff record with status TRAINING</li>
 *   <li>Assign staff to branch 470010</li>
 * </ol>
 *
 * @see Staff
 * @see StaffDTO
 * @see StaffStatus
 * @see StaffService
 * @see UserPortService
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class AddStaffUseCase {

    private final StaffService staffService;
    private final UserPortService userPortService;

    public boolean execute(@Valid StaffDTO staffDTO) {
        checkUserExists(staffDTO.username());

        var isAdded = false;

        try {

            Staff staff = new Staff(staffDTO.username(), StaffStatus.TRAINING, staffDTO.branchId());
            isAdded = staffService.addStaff(staff);

        } catch (EntityAlreadyExistException e) {

            log.error("Error adding staff, staff already exists in this branch", e);
             throw new  ResponseStatusException(HttpStatus.CONFLICT,"Staff already exists in this branch.",e);
        }
        catch (Exception e) {

            log.error("Error adding staff {}", staffDTO.username(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",e);
        }

        return isAdded;
    }

    void checkUserExists(String username) {
        userPortService.execute(username)
                .orElseThrow(() ->{
                    log.error("Username {} does not exist in this branch", username);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found at company system.");
                });
    }
}