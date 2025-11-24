package lunga.appointmentbooking.exeption;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class UnAuthorizedException extends ClientErrorException {
    public UnAuthorizedException(String message) {
        super(message, Response.Status.UNAUTHORIZED);
    }

}
