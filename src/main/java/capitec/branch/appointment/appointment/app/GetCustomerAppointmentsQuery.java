package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Query to retrieve appointments for a specific customer.
 */
public record GetCustomerAppointmentsQuery(
        @NotBlank(message = "Customer username is required")
        @Size(min = 5, max = 50, message = "Customer username must be between 5 and 50 characters")
        String customerUsername,

        AppointmentStatus status,

        @Min(value = 0, message = "Offset cannot be negative")
        int offset,

        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 100, message = "Limit cannot exceed 100")
        int limit
) {
    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_OFFSET = 0;

    /**
     * Creates a query for all appointments of a customer (no status filter, default pagination).
     */
    public GetCustomerAppointmentsQuery(String customerUsername) {
        this(customerUsername, null, DEFAULT_OFFSET, DEFAULT_LIMIT);
    }

    /**
     * Creates a query with status filter and default pagination.
     */
    public GetCustomerAppointmentsQuery(String customerUsername, AppointmentStatus status) {
        this(customerUsername, status, DEFAULT_OFFSET, DEFAULT_LIMIT);
    }
}
