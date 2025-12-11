package capitec.branch.appointment.branch.domain;





import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Optional;

public interface BranchService {

    Branch add(@Valid Branch branch);
    Optional<Branch> getByBranchId(String branchId);

    boolean delete(String branchId);

    Collection<Branch> getAllBranch();
}
