package lunga.appointmentbooking.authentication.domain;

import java.util.Optional;

public interface AuthService {

    Optional<TokenResponse> login(String username, String password, String traceId);
    boolean verifyCurrentPassword(String username, String password,String traceId);
    Optional<TokenResponse> refreshAccessToken(String refreshToken, String traceId);
    void logout(String refreshToken, String traceId);

    Optional<TokenResponse> impersonateUser(String username);
}
