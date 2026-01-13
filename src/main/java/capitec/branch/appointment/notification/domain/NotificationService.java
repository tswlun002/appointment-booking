package capitec.branch.appointment.notification.domain;


import capitec.branch.appointment.appointment.app.AppointmentBookedEvent;
import capitec.branch.appointment.appointment.app.AppointmentStateChangedEvent;
import capitec.branch.appointment.appointment.app.CustomerRescheduledAppointmentEvent;
import capitec.branch.appointment.exeption.MailSenderException;
import capitec.branch.appointment.notification.app.port.BranchDetails;
import capitec.branch.appointment.notification.app.port.CustomerDetails;

import java.util.Set;

public interface NotificationService {

    void sendOTPEmail(String hostEmail, Set<String> recipients,String subject, String emailTemplate, String traceId) throws MailSenderException;

    void sendAttendanceStateTransitionEmail(CustomerDetails user, AppointmentStateChangedEvent event);

    void sendBookingConfirmation(BranchDetails branch, CustomerDetails user, AppointmentBookedEvent event);

    void sendAppointmentRescheduled(BranchDetails branch, CustomerDetails user, CustomerRescheduledAppointmentEvent event);
}
