package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.app.port.BranchOperationHoursPort;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverrideService;
import capitec.branch.appointment.exeption.BranchIsClosedException;
import capitec.branch.appointment.exeption.BranchLocationServiceException;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

@UseCase
@Slf4j
@Validated
@RequiredArgsConstructor
public class AddBranchOperationHourOverride {
    private static final String COUNTRY = "South Africa";
    private final BranchService branchService;
    private final OperationHoursOverrideService operationHoursOverrideService;
    private final BranchOperationHoursPort branchOperationHoursPort;

    public boolean execute(String branchId, @Valid BranchOperationHourOverrideDTO dto) {

        return executeWithExceptionHandling(dto, () -> {

            validateBranchExists(branchId);

            Branch branch = getByBranchIdOrThrow(branchId);

            OperationHoursOverride override = new OperationHoursOverride(
                    dto.effectiveDate(), dto.openTime(), dto.closingTime(),
                    dto.isClosed(), dto.reason());

            branch.updateOperationHoursOverride(override);

            return operationHoursOverrideService.addBranchOperationHoursOverride(
                    override.effectiveDate(), branch);
        });
    }

    private void validateBranchExists(String branchId) {
        if (!branchOperationHoursPort.checkExist(COUNTRY, branchId)) {
            log.warn("Branch does not exist in the system, branchId: {}", branchId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch is not found.");
        }
    }

    private Branch getByBranchIdOrThrow(String branchId) {
        return branchService.getByBranchId(branchId)
                .orElseThrow(() -> {
                    log.warn("Branch not found, branchId: {}", branchId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch is not found.");
                });
    }

    private Boolean executeWithExceptionHandling(BranchOperationHourOverrideDTO dto, Supplier<Boolean> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid branch operation hour override input: {}", dto, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (BranchLocationServiceException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
        } catch (BranchIsClosedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is closed.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Internal server error.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.", e);
        }
    }
}
