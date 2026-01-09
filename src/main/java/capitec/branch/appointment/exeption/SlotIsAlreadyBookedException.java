package capitec.branch.appointment.exeption;


import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public final class SlotIsAlreadyBookedException extends ClientErrorException {
    public SlotIsAlreadyBookedException(String message) {
        super(message, Response.Status.CONFLICT);
    }
    public SlotIsAlreadyBookedException(String message,Throwable cause) {
        super(message, Response.Status.CONFLICT,cause);
    }
}
