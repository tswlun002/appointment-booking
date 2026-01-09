package capitec.branch.appointment.exeption;


import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public final class SlotFullyBookedException extends ClientErrorException {
    public SlotFullyBookedException(String message) {
        super(message, Response.Status.CONFLICT);
    }
    public SlotFullyBookedException(String message, Throwable cause) {
        super(message, Response.Status.CONFLICT,cause);
    }
}
