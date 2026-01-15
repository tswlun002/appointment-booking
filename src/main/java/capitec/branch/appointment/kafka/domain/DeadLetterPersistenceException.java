package capitec.branch.appointment.kafka.domain;

/**
 * Exception thrown when persisting a dead letter event fails.
 */
public class DeadLetterPersistenceException extends RuntimeException {

    public DeadLetterPersistenceException(Throwable cause) {
        super("Failed to persist dead letter event", cause);
    }

    public DeadLetterPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}

