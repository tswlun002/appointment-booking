package capitec.branch.appointment.notification.app;

import capitec.branch.appointment.exeption.MailSenderException;
import capitec.branch.appointment.exeption.NonRetryableException;
import capitec.branch.appointment.notification.app.port.BranchDetails;
import capitec.branch.appointment.notification.app.port.BranchLookup;
import capitec.branch.appointment.notification.app.port.CustomerDetails;
import capitec.branch.appointment.notification.app.port.CustomerLookup;
import capitec.branch.appointment.notification.domain.AppointmentBookedEmail;
import capitec.branch.appointment.notification.domain.AppointmentStatusUpdatesEmail;
import capitec.branch.appointment.notification.domain.NotificationService;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.sharekernel.EventTrigger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.validation.annotation.Validated;
import java.util.Objects;
import java.util.Set;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class SendAppointmentEmailUseCase {

    private final BranchLookup branchLookup;
    private final CustomerLookup customerLookup;
    private final NotificationService notificationService;

    @Value("${mail.username}")
    private String hostEmail;

    @EventListener(AppointmentBookedEmail.class)
    public void onAppointmentBooked(@Valid AppointmentBookedEmail event) throws MailSenderException {

        log.info("Sending appointment email, traceId:{}", event.traceId());

        CustomerDetails user = customerLookup.findByUsername(event.customerUsername());
        BranchDetails branchDetails = branchLookup.findById(event.branchId());

        String subject;
        String body;

        switch (event.eventType()) {
            case APPOINTMENT_BOOKED -> {
                subject = EmailTemplates.BOOKING_CONFIRMATION_SUBJECT;
                body = EmailTemplates.BOOKING_CONFIRMATION_BODY
                        .replaceAll("\\{customerName}", user.fullname())
                        .replaceAll("\\{branchName}", branchDetails.branchName())
                        .replaceAll("\\{branchAddress}", branchDetails.address())
                        .replaceAll("\\{appointmentReference}", event.reference())
                        .replaceAll("\\{date}", event.day().toString())
                        .replaceAll("\\{startTime}", event.startTime().toString())
                        .replaceAll("\\{endTime}", event.endTime().toString())
                        .replaceAll("\\{branch}", branchDetails.branchName());
            }
            case APPOINTMENT_RESCHEDULED -> {
                subject = EmailTemplates.RESCHEDULE_CONFIRMATION_SUBJECT;
                body = EmailTemplates.RESCHEDULE_CONFIRMATION_BODY
                        .replaceAll("\\{customerName}", user.fullname())
                        .replaceAll("\\{branchName}", branchDetails.branchName())
                        .replaceAll("\\{address}", branchDetails.address())
                        .replaceAll("\\{referenceNumber}", event.reference())
                        .replaceAll("\\{date}", event.day().toString())
                        .replaceAll("\\{startTime}", event.startTime().toString())
                        .replaceAll("\\{endTime}", event.endTime().toString());
            }
            default -> {
                log.error("Unsupported event type: {} - this is a code defect. Event: {}, traceId: {}", event.eventType(), event, event.traceId());
                throw new NonRetryableException("Unsupported event type:"+event.eventType());
            }
        }

        notificationService.sendEmail(hostEmail, Set.of(user.email()), subject, body, event.traceId());
    }

    @EventListener(AppointmentStatusUpdatesEmail.class)
    public void onAppointmentStatusUpdates(@Valid AppointmentStatusUpdatesEmail event) throws MailSenderException {

        log.info("Sending appointment status updates email, traceId:{}", event.traceId());
        CustomerDetails user = customerLookup.findByUsername(event.customerUsername());
        BranchDetails branchDetails = branchLookup.findById(event.branchId());
        String date = event.createdAt().toLocalDate().toString();
        String time = event.createdAt().toLocalTime().toString();

        String subject =null;
        String body = null;

        switch (event.eventType()) {
            case ATTENDED_APPOINTMENT -> {
                switch (event.toState().toUpperCase()) {
                    case "CHECKED_IN" -> {
                        subject = EmailTemplates.STATUS_CHECKIN_SUBJECT;
                        body = EmailTemplates.STATUS_CHECKIN_BODY
                                .replaceAll("\\{customerName}", user.fullname())
                                .replaceAll("\\{startTime}", time)
                                .replaceAll("\\{referenceNumber}", event.reference())
                                .replaceAll("\\{branchName}", branchDetails.branchName());
                    }
                    case "IN_PROGRESS" -> {
                        subject = EmailTemplates.STATUS_INPROGRESS_SUBJECT;
                        body = EmailTemplates.STATUS_INPROGRESS_BODY
                                .replaceAll("\\{customerName}", user.fullname())
                                .replaceAll("\\{referenceNumber}", event.reference())
                                .replaceAll("\\{branchName}", branchDetails.branchName());
                    }
                    case "COMPLETED" -> {
                        subject = EmailTemplates.STATUS_COMPLETED_SUBJECT;
                        body = EmailTemplates.STATUS_COMPLETED_BODY
                                .replaceAll("\\{customerName}", user.fullname())
                                .replaceAll("\\{date}", date)
                                .replaceAll("\\{referenceNumber}", event.reference())
                                .replaceAll("\\{branchName}", branchDetails.branchName());
                    }
                    default -> {
                        log.error("Unsupported toState: {} for ATTENDED_APPOINTMENT. Event: {}", event.toState(), event);
                         throw new NonRetryableException("Unsupported toState:"+event.toState());
                    }
                }
            }
            case APPOINTMENT_CANCELED -> {

                var trigger = EventTrigger.valueOf(event.triggeredBy().toUpperCase());

                switch (trigger) {
                    case EventTrigger.STAFF->{
                        subject = EmailTemplates.CANCEL_BY_STAFF_SUBJECT;
                        body = EmailTemplates.CANCEL_BY_STAFF_BODY;
                    }
                    case EventTrigger.CUSTOMER -> {
                        subject = EmailTemplates.CANCEL_BY_CUSTOMER_SUBJECT;
                        body = EmailTemplates.CANCEL_BY_CUSTOMER_BODY;
                    }
                    default -> {
                        log.error("Unsupported fromState: {} for APPOINTMENT_CANCELED. Event: {}", event.fromState(), event);
                        throw new NonRetryableException("Unsupported fromState:"+event.fromState());
                    }
                }

                body = body.replaceAll("\\{customerName}", user.fullname())
                        .replaceAll("\\{date}", date)
                        .replaceAll("\\{startTime}", time)
                        .replaceAll("\\{referenceNumber}", event.reference())
                        .replaceAll("\\{branchName}", branchDetails.branchName());
            }

        }

        notificationService.sendEmail(hostEmail, Set.of(user.email()), Objects.requireNonNull(subject), Objects.requireNonNull(body), event.traceId());
    }
}
