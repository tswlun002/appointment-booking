package capitec.branch.appointment.branch.domain;


import jakarta.validation.Valid;

import java.util.Optional;

public interface BranchService {

    void add(@Valid Branch branch);
    Optional<Branch> getByBranchId(String branchId);
}
