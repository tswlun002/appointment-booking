package capitec.branch.appointment.utils;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

/**
 * Utility class for executing use case operations with standardized exception handling.
 * Reduces boilerplate code across use cases by providing a common pattern for:
 * - EntityAlreadyExistException → 409 Conflict
 * - Validation errors (IllegalArgumentException, IllegalStateException) → 400 Bad Request
 * - Domain exceptions → 500 Internal Server Error (with domain message)
 * - ResponseStatusException → Re-thrown as-is
 * - Unexpected exceptions → 500 Internal Server Error (with generic message)
 */
@Slf4j
public final class UseCaseExecutor {

    private UseCaseExecutor() {
        // Utility class - prevent instantiation
    }

    /**
     * Executes a use case operation with standardized exception handling.
     *
     * @param operation       The operation to execute
     * @param operationName   Name of the operation for error messages (e.g., "Login", "Role creation")
     * @param domainException The domain exception class to catch
     * @param logContext      Context for logging (e.g., "email: test@test.com, traceId: 123")
     * @param <T>             Return type of the operation
     * @param <E>             Domain exception type
     * @return Result of the operation
     */
    public static <T, E extends RuntimeException> T execute(
            Supplier<T> operation,
            String operationName,
            Class<E> domainException,
            String logContext) {

        try {
            return operation.get();
        } catch (EntityAlreadyExistException e) {
            log.warn("Entity already exists. {}, error: {}", logContext, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. {}, error: {}", logContext, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            if (domainException.isInstance(e)) {
                log.error("Domain error. {}, error: {}", logContext, e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
            log.error("Unexpected error during {}. {}", operationName.toLowerCase(), logContext, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    operationName + " failed. Please try again later.", e);
        }
    }

    /**
     * Executes a void use case operation with standardized exception handling.
     *
     * @param operation       The operation to execute
     * @param operationName   Name of the operation for error messages
     * @param domainException The domain exception class to catch
     * @param logContext      Context for logging
     * @param <E>             Domain exception type
     */
    public static <E extends RuntimeException> void executeVoid(
            Runnable operation,
            String operationName,
            Class<E> domainException,
            String logContext) {

        execute(() -> {
            operation.run();
            return null;
        }, operationName, domainException, logContext);
    }
}
