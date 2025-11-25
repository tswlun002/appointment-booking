package capitec.branch.appointment.user.app;



import capitec.branch.appointment.authentication.domain.TokenResponse;

public interface ImpersonateUserLogin {
    TokenResponse adminImpersonateUserLogin(String username, String traceId);
}