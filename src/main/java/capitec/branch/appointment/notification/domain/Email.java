package capitec.branch.appointment.notification.domain;



public sealed interface Email permits ConfirmationEmail, OTPEmail {

    String email();
    String fullname();
    String traceId();
    Notification.EventType eventType();
}
