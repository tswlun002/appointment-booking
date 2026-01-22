package capitec.branch.appointment.branch.app.port;

import java.time.LocalDate;
import java.util.Optional;

public interface BranchOperationHoursPort {

    Optional<OperationHourDetails> getOperationHours(String country,String branchId, LocalDate day);
    boolean checkExist(String country, String branchId);
}
