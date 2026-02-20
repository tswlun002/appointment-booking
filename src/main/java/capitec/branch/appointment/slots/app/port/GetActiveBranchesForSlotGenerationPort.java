package capitec.branch.appointment.slots.app.port;


import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

public interface GetActiveBranchesForSlotGenerationPort {
    Collection<BranchOperationTimesDetails> execute(String country, LocalDate fromDate);

    Collection<BranchOperationTimesDetails> execute(Set<String> branches,String country, LocalDate date);
}
