package capitec.branch.appointment.user.app.port;

import capitec.branch.appointment.authentication.domain.TokenResponse;

/**
 * Port for authentication operations.
 * Implemented by Authentication context infrastructure.
 */
public interface AuthenticationPort {

    /**
     * Impersonates a user to auto-login after verification.
     *
     * @param username the username to impersonate
     * @param traceId  trace identifier for logging
     * @return the token response for the impersonated user
     */
    TokenResponse impersonateUser(String username, String traceId);
}
