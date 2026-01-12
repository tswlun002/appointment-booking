package capitec.branch.appointment.notification.infrastructure.adapter;

import capitec.branch.appointment.branch.app.GetBranchQuery;
import capitec.branch.appointment.notification.app.port.BranchDetails;
import capitec.branch.appointment.notification.app.port.BranchLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class BranchLookupAdapter implements BranchLookup {

    private final GetBranchQuery getBranchQuery;

    @Override
    public BranchDetails findById(String branchId) {
        var branch = getBranchQuery.execute(branchId);
        return new BranchDetails(branch.getBranchId(),branch.getAddress().toString());
    }
}
