package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.app.port.BranchQueryPort;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverrideService;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

/**
 * Use case for adding or updating operation hours override for a specific branch and date.
 *
 * <p>Operation hours overrides allow branch administrators to temporarily change the standard
 * operating hours for specific dates (e.g., early closing on holidays, special events, or
 * emergency closures). These overrides take precedence over the default branch operating hours
 * when generating appointment slots.</p>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Fetches the branch from database (throws 404 if not found)</li>
 *   <li>Creates an {@link OperationHoursOverride} domain object with the override details</li>
 *   <li>Updates the branch with the new operation hours override</li>
 *   <li>Persists the override to the database</li>
 * </ol>
 *
 * <h2>Input ({@link BranchOperationHourOverrideDTO}):</h2>
 * <ul>
 *   <li><b>effectiveDate</b> - The date when this override applies (must be today or future)</li>
 *   <li><b>openTime</b> - Override opening time for the branch</li>
 *   <li><b>closingTime</b> - Override closing time for the branch</li>
 *   <li><b>isClosed</b> - If true, the branch is closed on this date</li>
 *   <li><b>reason</b> - Mandatory reason for the override (e.g., "Public Holiday", "Staff Training")</li>
 * </ul>
 *
 * <h2>Business Rules:</h2>
 * <ul>
 *   <li>Branch must exist</li>
 *   <li>Effective date must be today or in the future (cannot override past dates)</li>
 *   <li>Open time must be before closing time</li>
 *   <li>If branch is marked as closed, a reason must be provided</li>
 * </ul>
 *
 * <h2>Example Use Case:</h2>
 * <p>Admin wants to close branch 470010 early on December 24th for Christmas Eve:</p>
 * <pre>
 * {
 *   "effectiveDate": "2026-12-24",
 *   "openTime": "08:00",
 *   "closingTime": "13:00",
 *   "isClosed": false,
 *   "reason": "Christmas Eve - Early closing"
 * }
 * </pre>
 *
 * @see OperationHoursOverride
 * @see BranchOperationHourOverrideDTO
 * @see OperationHoursOverrideService
 */
@UseCase
@Slf4j
@Validated
@RequiredArgsConstructor
public class AddBranchOperationHourOverride {
    private final BranchQueryPort branchQueryPort;
    private final OperationHoursOverrideService operationHoursOverrideService;

    public boolean execute(String branchId, @Valid BranchOperationHourOverrideDTO dto) {

        return executeWithExceptionHandling(dto, () -> {


            Branch branch = getByBranchIdOrThrow(branchId);

            OperationHoursOverride override = new OperationHoursOverride(
                    dto.effectiveDate(), dto.openTime(), dto.closingTime(),
                    dto.isClosed(), dto.reason());

            branch.updateOperationHoursOverride(override);

            return operationHoursOverrideService.addBranchOperationHoursOverride(
                    override.effectiveDate(), branch);
        });
    }


    private Branch getByBranchIdOrThrow(String branchId) {
        return branchQueryPort.findByBranchId(branchId)
                .orElseThrow(() -> {
                    log.warn("Branch not found, branchId: {}", branchId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch is not found.");
                });
    }

    private Boolean executeWithExceptionHandling(BranchOperationHourOverrideDTO dto, Supplier<Boolean> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException | IllegalStateException | ConstraintViolationException e) {
            log.warn("Invalid branch operation hour override input: {}", dto, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getLocalizedMessage(), e);
        }catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Internal server error.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.", e);
        }
    }
}
