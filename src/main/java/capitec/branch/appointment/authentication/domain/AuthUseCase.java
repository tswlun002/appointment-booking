package capitec.branch.appointment.authentication.domain;

import capitec.branch.appointment.authentication.infrastructure.api.LoginDTO;

public interface AuthUseCase {
    TokenResponse login(LoginDTO loginDTO, String traceId);
    TokenResponse refreshAccessToken(String refreshToken, String traceId);
    boolean verifyCurrentPassword(String username, String password,String traceId);
    TokenResponse adminImpersonateUser(String username,String traceId);
    void logout(String refreshToken,String traceId);
}
