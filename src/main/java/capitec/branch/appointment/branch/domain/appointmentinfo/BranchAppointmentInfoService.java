package capitec.branch.appointment.branch.domain.appointmentinfo;


import capitec.branch.appointment.branch.domain.Branch;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public interface BranchAppointmentInfoService {

    boolean addBranchAppointmentConfigInfo(@NotNull LocalDate day, @Valid Branch branch);
}
