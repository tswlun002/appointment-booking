package capitec.branch.appointment.exeption;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.springframework.http.HttpStatus;

public class OTPExpiredException extends ClientErrorException {
    private HttpStatus status;

    public OTPExpiredException(String message) {
        super(message, Response.Status.UNAUTHORIZED);
        this.status = HttpStatus.UNAUTHORIZED;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
