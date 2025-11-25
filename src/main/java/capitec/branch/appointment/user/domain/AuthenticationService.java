package capitec.branch.appointment.user.domain;

import capitec.branch.appointment.user.app.TokenResponse;

import java.util.Optional;

public interface AuthenticationService {

    Optional<TokenResponse> adminImpersonateUserLogin( String username, String traceId, String adminToken);

}
