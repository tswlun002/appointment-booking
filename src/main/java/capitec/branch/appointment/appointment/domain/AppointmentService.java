package capitec.branch.appointment.appointment.domain;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentService {

    Appointment book(@Valid Appointment appointment);
    Collection<Appointment> getUnAttendedAppointments( LocalDate appointmentDate,  UUID lastProcessedId, int limit);
    Appointment update(@Valid Appointment appointment);
    Collection<Appointment> update( Collection<Appointment> appointment);

    Optional<Appointment> findById(UUID appointmentId);
    boolean deleteAppointment(UUID appointmentId);
    Collection<Appointment> branchAppointments(String branchId, int pageNumber, int pageSize);

    Optional<Appointment> getUserActiveAppointment(String branchId, LocalDate day, String customerUsername);

    Collection<Appointment> findByCustomerUsername(String customerUsername, AppointmentStatus status, int offset, int limit);
}
