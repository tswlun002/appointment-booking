package capitec.branch.appointment.notification.app.port;

import java.util.Optional;

public interface BranchLookup {
    Optional<BranchDetails> findById(String branchId);
}
