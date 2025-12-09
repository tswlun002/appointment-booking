package capitec.branch.appointment.branch.domain.appointmentinfo;


import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.day.domain.DayType;

public interface BranchAppointmentInfoService {

    boolean addBranchAppointmentConfigInfo(DayType dayType, Branch branch);
}
