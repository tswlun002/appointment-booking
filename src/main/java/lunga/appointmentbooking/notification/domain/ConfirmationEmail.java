package lunga.appointmentbooking.notification.domain;




public record ConfirmationEmail(
        String email,
        String fullname,
        String traceId,
        Notification.EventType eventType)  implements Email{

}
