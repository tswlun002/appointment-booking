package lunga.appointmentbooking.exeption;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class TokenExpiredException extends ClientErrorException {
    public TokenExpiredException(String message) {
        super(message, Response.Status.UNAUTHORIZED);
    }

}
