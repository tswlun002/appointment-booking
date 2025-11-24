package lunga.appointmentbooking.notification.infrastructure.springmail;



import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunga.appointmentbooking.exeption.MailSenderException;
import lunga.appointmentbooking.notification.domain.SendEmail;
import org.eclipse.angus.mail.util.MailConnectException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpringMailSender  implements SendEmail {

    private final JavaMailSender mailSender;

    @Override
    public void sendOTPEmail(String hostEmail, Set<String> recipients, String subject, String emailTemplate, String traceId) throws MailSenderException{

        try {

            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(new InternetAddress(hostEmail));
            String emailRecipients = String.join(",", recipients);
            InternetAddress address = new InternetAddress(emailRecipients);
            message.addRecipient(Message.RecipientType.TO, address);
            message.setSubject(subject);
            MimeMessageHelper helper = new MimeMessageHelper(message, true, String.valueOf(Charset.defaultCharset()));
            helper.setText(emailTemplate, true);
            mailSender.send(message);

        } catch (Exception e) {

            if (e instanceof MailConnectException || e instanceof MailSendException
                    || e.getCause() instanceof MailSendException || e.getCause() instanceof IOException) {

                log.info("Failed to send email from event:{},traceId:{}.\nMailSenderException is thrown : {}", subject, traceId, e.getMessage(), e);

                throw new MailSenderException(e.getCause().getMessage(), e);
            }
            log.info("Failed to send email from event:{}, traceId:{}. /nDeadException is thrown : {}", subject, traceId, e.getMessage(), e);

            throw new RuntimeException("Dead for mail sender exception", e);
        }
    }
}
