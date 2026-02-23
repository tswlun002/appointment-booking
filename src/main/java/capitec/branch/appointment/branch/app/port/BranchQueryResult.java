package capitec.branch.appointment.branch.app.port;

import capitec.branch.appointment.branch.domain.Branch;

import java.util.List;

/**
 * Result of paginated branch query containing data and total count.
 */
public record BranchQueryResult(
        List<Branch> branches,
        int totalCount
) {
    public static BranchQueryResult of(List<Branch> branches, int totalCount) {
        return new BranchQueryResult(branches, totalCount);
    }

    public static BranchQueryResult empty() {
        return new BranchQueryResult(List.of(), 0);
    }
}
