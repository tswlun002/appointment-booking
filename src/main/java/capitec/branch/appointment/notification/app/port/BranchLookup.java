package capitec.branch.appointment.notification.app.port;

public interface BranchLookup {
    BranchDetails findById(String branchId);
}
