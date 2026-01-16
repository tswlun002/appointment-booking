package capitec.branch.appointment.exeption;

public class NonRetryableException extends RuntimeException {
    public NonRetryableException(String message) {
        super(message);
    }
}