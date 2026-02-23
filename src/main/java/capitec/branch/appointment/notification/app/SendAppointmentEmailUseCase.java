package capitec.branch.appointment.notification.app;

import capitec.branch.appointment.exeption.MailSenderException;
import capitec.branch.appointment.exeption.NonRetryableException;
import capitec.branch.appointment.notification.app.port.BranchDetails;
import capitec.branch.appointment.notification.app.port.BranchLookup;
import capitec.branch.appointment.notification.app.port.CustomerDetails;
import capitec.branch.appointment.notification.app.port.CustomerLookup;
import capitec.branch.appointment.notification.domain.AppointmentBookedEmail;
import capitec.branch.appointment.notification.domain.AppointmentStatusUpdatesEmail;
import capitec.branch.appointment.notification.domain.Notification;
import capitec.branch.appointment.notification.domain.NotificationService;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.sharekernel.EventTrigger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Set;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class SendAppointmentEmailUseCase {

    private static final String APPOINTMENT_TEMPLATE = "email/appointment-notification";

    private static final Map<String, String> EMAIL_SUBJECTS = Map.of(
            "APPOINTMENT_BOOKED", "Appointment Confirmation",
            "APPOINTMENT_RESCHEDULED", "Appointment Rescheduled",
            "CHECKED_IN", "Check-In Confirmation",
            "IN_PROGRESS", "Appointment In Progress",
            "COMPLETED", "Appointment Completed",
            "CANCEL_BY_CUSTOMER", "Appointment Cancelled",
            "CANCEL_BY_STAFF", "Appointment Cancelled by Branch"
    );

    private final BranchLookup branchLookup;
    private final CustomerLookup customerLookup;
    private final NotificationService notificationService;
    private final TemplateEngine templateEngine;

    @Value("${mail.username}")
    private String hostEmail;

    @Value("${mail.support:support@capitec.co.za}")
    private String supportEmail;

    @EventListener(AppointmentBookedEmail.class)
    public void onAppointmentBooked(@Valid AppointmentBookedEmail event) throws MailSenderException {
        log.info("Sending appointment booked email. eventType: {}, traceId: {}", event.eventType(), event.traceId());

        CustomerDetails user = customerLookup.findByUsername(event.customerUsername(),event.traceId());
        BranchDetails branch = getBranchDetails(event.branchId());

        String eventType = event.eventType().name();
        validateEventType(eventType, event.traceId());

        Context context = new Context();
        context.setVariable("eventType", eventType);
        context.setVariable("customerName", user.fullname());
        context.setVariable("branchName", branch.branchName());
        context.setVariable("branchAddress", branch.address());
        context.setVariable("reference", event.reference());
        context.setVariable("date", event.day().toString());
        context.setVariable("startTime", event.startTime().toString());
        context.setVariable("endTime", event.endTime().toString());
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("subject", EMAIL_SUBJECTS.get(eventType));

        String body = templateEngine.process(APPOINTMENT_TEMPLATE, context);
        notificationService.sendEmail(hostEmail, Set.of(user.email()), EMAIL_SUBJECTS.get(eventType), body, event.traceId());

        log.info("Appointment booked email sent successfully. traceId: {}", event.traceId());
    }

    @EventListener(AppointmentStatusUpdatesEmail.class)
    public void onAppointmentStatusUpdates(@Valid AppointmentStatusUpdatesEmail event) throws MailSenderException {
        log.info("Sending appointment status update email. eventType: {}, traceId: {}", event.eventType(), event.traceId());

        CustomerDetails user = customerLookup.findByUsername(event.customerUsername(),event.traceId());
        BranchDetails branch = getBranchDetails(event.branchId());

        String eventType = resolveStatusEventType(event);
        validateEventType(eventType, event.traceId());

        String date = event.createdAt().toLocalDate().toString();
        String time = event.createdAt().toLocalTime().toString();

        Context context = new Context();
        context.setVariable("eventType", eventType);
        context.setVariable("customerName", user.fullname());
        context.setVariable("branchName", branch.branchName());
        context.setVariable("branchAddress", branch.address());
        context.setVariable("reference", event.reference());
        context.setVariable("date", date);
        context.setVariable("time", time);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("subject", EMAIL_SUBJECTS.get(eventType));

        String body = templateEngine.process(APPOINTMENT_TEMPLATE, context);
        notificationService.sendEmail(hostEmail, Set.of(user.email()), EMAIL_SUBJECTS.get(eventType), body, event.traceId());

        log.info("Appointment status update email sent successfully. traceId: {}", event.traceId());
    }

    private String resolveStatusEventType(AppointmentStatusUpdatesEmail event) {
        if (event.eventType() == Notification.AppointmentEventType.ATTENDED_APPOINTMENT) {
            return event.toState().toUpperCase();
        } else if (event.eventType() == Notification.AppointmentEventType.APPOINTMENT_CANCELED) {
            EventTrigger trigger = EventTrigger.valueOf(event.triggeredBy().toUpperCase());
            return switch (trigger) {
                case STAFF -> "CANCEL_BY_STAFF";
                case CUSTOMER -> "CANCEL_BY_CUSTOMER";
                default -> throw new NonRetryableException("Unsupported trigger: " + event.triggeredBy());
            };
        }
        throw new NonRetryableException("Unsupported event type: " + event.eventType());
    }

    private BranchDetails getBranchDetails(String branchId) {
        return branchLookup.findById(branchId)
                .orElseThrow(() -> {
                    log.warn("No branch found with id: {}", branchId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "No branch found.");
                });
    }

    private void validateEventType(String eventType, String traceId) {
        if (!EMAIL_SUBJECTS.containsKey(eventType)) {
            log.error("Unsupported event type: {}. traceId: {}", eventType, traceId);
            throw new NonRetryableException("Unsupported event type: " + eventType);
        }
    }
}
