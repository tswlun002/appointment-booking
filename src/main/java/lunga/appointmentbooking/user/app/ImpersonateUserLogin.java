package lunga.appointmentbooking.user.app;



import  lunga.appointmentbooking.authentication.domain.TokenResponse;

public interface ImpersonateUserLogin {
    TokenResponse adminImpersonateUserLogin(String username, String traceId);
}