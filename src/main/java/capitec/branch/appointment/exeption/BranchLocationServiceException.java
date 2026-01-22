package capitec.branch.appointment.exeption;


public class BranchLocationServiceException extends RuntimeException {

    public BranchLocationServiceException(String message) {
        super(message);
    }

    public BranchLocationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

