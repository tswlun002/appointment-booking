package capitec.branch.appointment.branch.app.port;

import capitec.branch.appointment.branch.domain.Branch;

import java.util.Optional;

/**
 * Port for read-only branch queries.
 * Queries are not business rules - they are data retrieval for presentation.
 */
public interface BranchQueryPort {

    Optional<Branch> findByBranchId(String branchId);
    Optional<Branch> findByBranchWithLatestDataById(String branchId);

    /**
     * Find all branches with pagination.
     * Returns both branches and total count in single query.
     */
    BranchQueryResult findAllActiveBranches(int offset, int limit);/**
     * Find all branches with pagination.
     * Returns both branches and total count in single query.
     */
    BranchQueryResult findAllBranches(int offset, int limit);

}
