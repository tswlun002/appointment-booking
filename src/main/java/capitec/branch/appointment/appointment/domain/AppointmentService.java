package capitec.branch.appointment.appointment.domain;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service for appointment business operations.
 * Contains only business rules - no query/pagination concerns.
 */
public interface AppointmentService {

    Appointment book(@Valid Appointment appointment);

    Appointment update(@Valid Appointment appointment);

    Collection<Appointment> update(Collection<Appointment> appointment);

    boolean deleteAppointment(UUID appointmentId);

    /**
     * Business rule: Get user's active appointment for a branch on a specific day.
     * Used to enforce one active appointment per user per branch per day.
     */
    Optional<Appointment> getUserActiveAppointment(String branchId, LocalDate day, String customerUsername);

    /**
     * Business rule: Get unattended appointments for marking as no-show.
     */
    Collection<Appointment> getUnAttendedAppointments(LocalDate appointmentDate, UUID lastProcessedId, int limit);
}
