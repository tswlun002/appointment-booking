package capitec.branch.appointment.sharekernel.retry;

import java.time.LocalDateTime;

/**
 * Shared retry configuration for dead letter processing.
 * Used by both kafka and event contexts to avoid cross-context dependencies.
 */
public final class RetryConfiguration {

    private RetryConfiguration() {
    }

    /**
     * Maximum number of retry attempts for dead letter events.
     */
    public static final int MAX_RETRY = 5;

    /**
     * Calculates the next retry time based on exponential backoff.
     *
     * @param retryCount Current retry count (1-based)
     * @return LocalDateTime when the next retry should be attempted
     */
    public static LocalDateTime calculateNextRetry(int retryCount) {
        long delaySeconds = switch (retryCount) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 40;
            default -> 60;
        };
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }
}
