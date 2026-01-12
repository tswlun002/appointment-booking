package capitec.branch.appointment.appointment.domain;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentService {

    Appointment book(@Valid Appointment appointment);
    Appointment update(@Valid Appointment appointment);
    boolean checkIn(UUID appointmentId);
    boolean startService(String staffRef,UUID appointmentId);
    boolean complete(UUID appointmentId);
    boolean cancelByCustomer(UUID appointmentId);
    boolean cancelByStaff(String staffRef,String reason,UUID appointmentId);
    boolean reschedule(UUID appointmentId,UUID newSlotId);
    boolean markAsNoShow(UUID appointmentId);
    Optional<Appointment> findById(UUID appointmentId);
    Collection<Appointment> customerAppointments(String username,AppointmentStatus status);
    boolean deleteAppointment(UUID appointmentId);

    Collection<Appointment> branchAppointments(String branchId, int pageNumber, int pageSize);

    Optional<Appointment> getUserActiveAppointment(String branchId, LocalDate day, String customerUsername);
}
