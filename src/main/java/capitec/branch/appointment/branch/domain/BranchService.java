package capitec.branch.appointment.branch.domain;

import jakarta.validation.Valid;

/**
 * Domain service for branch business operations.
 * Contains only business rules - no query/pagination concerns.
 */
public interface BranchService {

    Branch add(@Valid Branch branch);

    boolean delete(String branchId);

}
