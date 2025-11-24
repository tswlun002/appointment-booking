package lunga.appointmentbooking.notification.domain;


public record OTPEmail(String email, String fullname, String OTPCode, String traceId, Notification.EventType eventType)  implements Email{

}
