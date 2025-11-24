package lunga.appointmentbooking.user.domain;

import lunga.appointmentbooking.user.app.TokenResponse;

import java.util.Optional;

public interface AuthenticationService {

    Optional<TokenResponse> adminImpersonateUserLogin( String username, String traceId, String adminToken);

}
