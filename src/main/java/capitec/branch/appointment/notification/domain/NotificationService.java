package capitec.branch.appointment.notification.domain;


import capitec.branch.appointment.exeption.MailSenderException;
import capitec.branch.appointment.utils.CustomerEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;


import java.util.Set;

public interface NotificationService {

    void sendEmail( @CustomerEmail String hostEmail,
                    @NotEmpty Set<@CustomerEmail String> recipients,
                    @NotBlank String subject,
                    @NotBlank String emailTemplate,
                    @NotBlank String traceId) throws MailSenderException;

  //  void sendAppointmentEmail(String hostEmail,Set<String> recipientEmail,String bookingConfirmationSubject, String bookingConfirmationBody, String traceId) throws MailSenderException;

}
