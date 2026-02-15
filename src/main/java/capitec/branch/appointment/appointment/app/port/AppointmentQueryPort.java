package capitec.branch.appointment.appointment.app.port;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for read-only appointment queries.
 * Queries are not business rules - they are data retrieval for presentation.
 */
public interface AppointmentQueryPort {

    Optional<Appointment> findById(UUID appointmentId);

    /**
     * Find appointments by customer username with pagination.
     * Returns both appointments and total count in single query.
     */
    AppointmentQueryResult findByCustomerUsername(String customerUsername, AppointmentStatus status, int offset, int limit);

    Collection<Appointment> findByBranchId(String branchId, int offset, int limit);
}
