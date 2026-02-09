package capitec.branch.appointment.notification.infrastructure.springmail;

import capitec.branch.appointment.exeption.MailSenderException;
import capitec.branch.appointment.exeption.NonRetryableException;
import capitec.branch.appointment.notification.domain.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.util.MailConnectException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class SpringMailSender implements NotificationService {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(
            @NotBlank @Email String hostEmail,
            @NotEmpty Set<@Email String> recipients,
            @NotBlank String subject,
            @NotBlank String emailTemplate,
            @NotBlank String traceId) throws MailSenderException {

        log.debug("Sending email. subject: {}, recipients: {}, traceId: {}", subject, recipients.size(), traceId);

        try {
            MimeMessage message = createMimeMessage(hostEmail, recipients, subject, emailTemplate);
            mailSender.send(message);
            log.info("Email sent successfully. subject: {}, traceId: {}", subject, traceId);

        } catch (MailConnectException | MailSendException e) {
            log.error("Failed to send email - connection/send error. subject: {}, traceId: {}", subject, traceId, e);
            throw new MailSenderException("Failed to connect to mail server", e);

        } catch (MailException e) {
            if (isRetryableException(e)) {
                log.error("Failed to send email - retryable error. subject: {}, traceId: {}", subject, traceId, e);
                throw new MailSenderException("Failed to send email", e);
            }
            log.error("Failed to send email - non-retryable error. subject: {}, traceId: {}", subject, traceId, e);
            throw new NonRetryableException("Failed to send email - non-retryable", e);

        } catch (MessagingException e) {
            log.error("Failed to create email message. subject: {}, traceId: {}", subject, traceId, e);
            throw new NonRetryableException("Failed to create email message", e);

        } catch (Exception e) {
            log.error("Unexpected error sending email. subject: {}, traceId: {}", subject, traceId, e);
            throw new NonRetryableException("Unexpected error sending email", e);
        }
    }

    private MimeMessage createMimeMessage(String hostEmail, Set<String> recipients, String subject, String emailTemplate)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        helper.setFrom(hostEmail);
        helper.setTo(recipients.toArray(String[]::new));
        helper.setSubject(subject);
        helper.setText(emailTemplate, true);

        return message;
    }

    private boolean isRetryableException(Exception e) {
        Throwable cause = e.getCause();
        return cause instanceof IOException
                || cause instanceof MailConnectException
                || cause instanceof MailSendException;
    }
}
