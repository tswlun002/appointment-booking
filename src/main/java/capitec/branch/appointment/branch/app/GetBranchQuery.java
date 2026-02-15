package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.app.port.BranchQueryPort;
import capitec.branch.appointment.branch.app.port.BranchQueryResult;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@UseCase
@RequiredArgsConstructor
public class GetBranchQuery {

    private final BranchQueryPort branchQueryPort;

    public Branch execute(String branchId) {
        
        return branchQueryPort.findByBranchId(branchId).orElseThrow(() -> {
            log.warn("Unable to find branch with id {}", branchId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
        });
    }

    public BranchQueryResult execute(int offset, int limit) {
        try {
            log.debug("Retrieving all branches, offset: {}, limit: {}", offset, limit);
            return branchQueryPort.findAll(offset, limit);
        } catch (Exception e) {
            log.error("Unable to retrieve all branches", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error during branch retrieval");
        }
    }
}