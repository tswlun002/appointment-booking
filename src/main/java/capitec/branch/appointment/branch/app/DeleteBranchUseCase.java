package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class DeleteBranchUseCase {

    private final BranchService branchService;

    public boolean execute(String branchId) {
        boolean deleted;
        try {
            log.debug("Deleting branch with id {}", branchId);
            deleted = branchService.delete(branchId);
        } catch (Exception e) {
            log.error("Unable to delete branch with id {}", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error during branch deletion");
        }
        return deleted;
    }
}