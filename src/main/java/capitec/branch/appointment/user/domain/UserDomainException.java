package capitec.branch.appointment.user.domain;

/**
 * Domain exception for user-related validation and business rule violations.
 */
public class UserDomainException extends RuntimeException {

    public UserDomainException(String message) {
        super(message);
    }

    public UserDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
