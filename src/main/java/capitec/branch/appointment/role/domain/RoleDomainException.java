package capitec.branch.appointment.role.domain;

/**
 * Domain exception for role-related errors.
 */
public class RoleDomainException extends RuntimeException {

    public RoleDomainException(String message) {
        super(message);
    }

    public RoleDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
