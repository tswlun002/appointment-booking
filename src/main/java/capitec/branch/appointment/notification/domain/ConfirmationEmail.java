package capitec.branch.appointment.notification.domain;




public record ConfirmationEmail(
        String email,
        String fullname,
        String traceId,
        Notification.UserEventType userEventType)  implements Email{

}
