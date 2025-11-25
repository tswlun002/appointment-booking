package capitec.branch.appointment.exeption;


import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public final class EntityAlreadyExistException extends ClientErrorException {

    public EntityAlreadyExistException(String message) {
        super(message, Response.Status.CONFLICT);
    }
}
