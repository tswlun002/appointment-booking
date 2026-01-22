package capitec.branch.appointment.branch.domain.operationhours;

import capitec.branch.appointment.branch.domain.Branch;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public interface OperationHoursOverrideService {
    boolean addBranchOperationHoursOverride(@NotNull LocalDate day, @Valid Branch branch);
}
