package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;


@Slf4j
@UseCase
@RequiredArgsConstructor
public class GetBranchQuery {

    private final BranchService branchService;

    public Branch execute(String branchId) {
        
        return branchService.getByBranchId(branchId).orElseThrow(() -> {
            log.warn("Unable to find branch with id {}", branchId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
        });
    }

    public Collection<Branch> execute() {

        Collection<Branch> branches ;
        try {
            log.debug("Retrieving all branches");
            branches = branchService.getAllBranch();
        } catch (Exception e) {
            log.error("Unable to retrieve all branches", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error during branch retrieval");
        }
        return branches;
    }
}