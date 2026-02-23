package capitec.branch.appointment.event.app.port;

public interface UserEventListenerPort {
    void handleUserVerifiedEvent(String username, String email, String fullName,String otp, String traceId);
    void handleDeleteUserEvent(String username, String email, String fullname, String otp, String traceId);
    void handlePasswordUpdatedEvent(String username, String email, String fullname, String otp, String traceId);
}
