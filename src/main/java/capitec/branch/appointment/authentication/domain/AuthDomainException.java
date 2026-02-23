package capitec.branch.appointment.authentication.domain;

/**
 * Domain exception for authentication-related errors.
 */
public class AuthDomainException extends RuntimeException {

    public AuthDomainException(String message) {
        super(message);
    }

    public AuthDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
