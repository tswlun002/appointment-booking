package capitec.branch.appointment.notification.domain;



public sealed interface Email<EventType> permits AppointmentBookedEmail, AppointmentStatusUpdatesEmail, ConfirmationEmail, OTPEmail {

    String email();
    String fullname();
    String traceId();
    EventType eventType();
}
