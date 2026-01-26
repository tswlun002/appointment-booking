package capitec.branch.appointment.slots.app.port;


import java.time.LocalDate;
import java.util.Collection;

public interface GetActiveBranchesForSlotGenerationPort {
    Collection<BranchOperationTimesDetails> execute(String country, LocalDate fromDate);
}
