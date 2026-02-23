package capitec.branch.appointment.appointment.app.port;

import java.util.Optional;

/**
 * Port to fetch branch information from location context.
 */
public interface BranchInfoPort {

    Optional<BranchInfo> getBranchInfo(String branchId);

    record BranchInfo(String branchId, String name, String address) {}
}
