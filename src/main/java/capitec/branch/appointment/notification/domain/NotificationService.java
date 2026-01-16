package capitec.branch.appointment.notification.domain;


import capitec.branch.appointment.exeption.MailSenderException;


import java.util.Set;

public interface NotificationService {

    void sendEmail(String hostEmail, Set<String> recipients, String subject, String emailTemplate, String traceId) throws MailSenderException;

  //  void sendAppointmentEmail(String hostEmail,Set<String> recipientEmail,String bookingConfirmationSubject, String bookingConfirmationBody, String traceId) throws MailSenderException;

}
