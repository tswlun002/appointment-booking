package capitec.branch.appointment.notification.domain;


public record OTPEmail(
        String email,
        String fullname,
        String OTPCode,
        String traceId,
        Notification.UserEventType eventType
)  implements Email<Notification.UserEventType>
{

}
