package capitec.branch.appointment.notification.app;


import capitec.branch.appointment.exeption.MailSenderException;
import capitec.branch.appointment.notification.domain.ConfirmationEmail;
import capitec.branch.appointment.notification.domain.Notification;
import capitec.branch.appointment.notification.domain.NotificationService;
import capitec.branch.appointment.notification.domain.OTPEmail;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class EmailSendUseCase {

    private static final String USER_NOTIFICATION_TEMPLATE = "email/user-notification";
    private static final Map<Notification.UserEventType, String> EMAIL_SUBJECTS = new EnumMap<>(Notification.UserEventType.class);

    static {
        EMAIL_SUBJECTS.put(Notification.UserEventType.REGISTRATION_EVENT, "Verification email");
        EMAIL_SUBJECTS.put(Notification.UserEventType.EMAIL_VERIFIED_EVENT, "Email verified");
        EMAIL_SUBJECTS.put(Notification.UserEventType.COMPLETE_REGISTRATION_EVENT, "Welcome to Capitec Appointment Booking");
        EMAIL_SUBJECTS.put(Notification.UserEventType.PASSWORD_RESET_REQUEST_EVENT, "Password Change Request");
        EMAIL_SUBJECTS.put(Notification.UserEventType.PASSWORD_UPDATED_EVENT, "Important Security Notice – Password Successfully Updated");
        EMAIL_SUBJECTS.put(Notification.UserEventType.DELETE_ACCOUNT_REQUEST_EVENT, "Important Security Notice – Account Deletion Request");
        EMAIL_SUBJECTS.put(Notification.UserEventType.DELETE_ACCOUNT_EVENT, "Account Successfully Deleted");
    }

    private final NotificationService notificationService;
    private final TemplateEngine templateEngine;

    @Value("${mail.username}")
    private String hostEmail;

    @Value("${mail.support:support@capitec.co.za}")
    private String supportEmail;

    @EventListener(OTPEmail.class)
    public void sendOTPEmail(@Valid OTPEmail event) throws MailSenderException {
        log.info("Sending OTP email. eventType: {}, traceId: {}", event.eventType(), event.traceId());

        String subject = getSubject(event.eventType());
        String body = buildEmailBody(event.eventType(), event.fullname(), event.OTPCode());
        notificationService.sendEmail(hostEmail, Set.of(event.email()), subject, body, event.traceId());

        log.info("OTP email sent successfully. traceId: {}", event.traceId());
    }

    @EventListener(ConfirmationEmail.class)
    public void sendConfirmationEmail(@Valid ConfirmationEmail event) throws MailSenderException {
        log.info("Sending confirmation email. eventType: {}, traceId: {}", event.eventType(), event.traceId());

        String subject = getSubject(event.eventType());
        String body = buildEmailBody(event.eventType(), event.fullname(), null);
        notificationService.sendEmail(hostEmail, Set.of(event.email()), subject, body, event.traceId());

        log.info("Confirmation email sent successfully. traceId: {}", event.traceId());
    }

    private String getSubject(Notification.UserEventType eventType) {
        String subject = EMAIL_SUBJECTS.get(eventType);
        if (subject == null) {
            throw new IllegalArgumentException("No email subject found for event type: " + eventType);
        }
        return subject;
    }

    private String buildEmailBody(Notification.UserEventType eventType, String fullname, String otpCode) {
        Context context = new Context();
        context.setVariable("eventType", eventType.name());
        context.setVariable("fullname", fullname);
        context.setVariable("subject", getSubject(eventType));
        context.setVariable("supportEmail", supportEmail);

        if (otpCode != null) {
            context.setVariable("otpCode", otpCode);
        }

        return templateEngine.process(USER_NOTIFICATION_TEMPLATE, context);
    }
}
