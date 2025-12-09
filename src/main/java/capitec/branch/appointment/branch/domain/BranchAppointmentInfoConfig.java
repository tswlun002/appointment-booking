package capitec.branch.appointment.branch.domain;

import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;

public interface BranchAppointmentInfoConfig {

    BranchAppointmentInfo getAppointmentInfo(String branchId);
}
