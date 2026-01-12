package capitec.branch.appointment.branch.domain.appointmentinfo;


import capitec.branch.appointment.branch.domain.Branch;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface BranchAppointmentInfoService {

    boolean addBranchAppointmentConfigInfo(@NotNull DayType dayType, @Valid Branch branch);
}
