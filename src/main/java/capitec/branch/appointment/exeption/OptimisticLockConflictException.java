package capitec.branch.appointment.exeption;


import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public final class OptimisticLockConflictException extends ClientErrorException {
    public OptimisticLockConflictException(String message) {
        super(message, Response.Status.CONFLICT);
    }
    public OptimisticLockConflictException(String message, Throwable cause) {
        super(message, Response.Status.CONFLICT,cause);
    }
}
