package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.port.UpdateSlotStatePort;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Use case for booking a new appointment at a branch.
 *
 * <p>This use case handles the complete appointment booking flow, including slot reservation,
 * appointment creation, persistence, and event publishing for downstream notifications.</p>
 *
 * <h2>Input ({@link AppointmentDTO}):</h2>
 * <ul>
 *   <li><b>slotId</b> - The unique identifier of the slot to book</li>
 *   <li><b>branchId</b> - The branch where the appointment will take place</li>
 *   <li><b>customerUsername</b> - The username of the customer booking the appointment</li>
 *   <li><b>serviceType</b> - The type of service requested</li>
 *   <li><b>day</b> - The date of the appointment</li>
 *   <li><b>startTime</b> - The start time of the appointment slot</li>
 *   <li><b>endTime</b> - The end time of the appointment slot</li>
 * </ul>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Creates an {@link Appointment} domain object with status BOOKED</li>
 *   <li>Reserves the slot (increments booking count, validates capacity)</li>
 *   <li>Persists the appointment to the database</li>
 *   <li>Publishes a booking event for email notifications</li>
 * </ol>
 *
 * <h2>Business Rules:</h2>
 * <ul>
 *   <li>Slot must have available capacity</li>
 *   <li>Customer cannot have another appointment on the same day</li>
 *   <li>Slot must be in a valid state for booking (not expired, not full)</li>
 * </ul>
 *
 * <h2>Error Handling:</h2>
 * <ul>
 *   <li><b>BAD_REQUEST (400)</b> - Invalid appointment data or slot state</li>
 *   <li><b>CONFLICT (409)</b> - Customer already has an appointment on this day</li>
 *   <li><b>INTERNAL_SERVER_ERROR (500)</b> - Unexpected errors during booking</li>
 * </ul>
 *
 * <h2>Example Use Case:</h2>
 * <p>Customer books an appointment for account inquiry:</p>
 * <pre>
 * {
 *   "slotId": "550e8400-e29b-41d4-a716-446655440000",
 *   "branchId": "470010",
 *   "customerUsername": "john.doe",
 *   "serviceType": "ACCOUNT_INQUIRY",
 *   "day": "2026-02-20",
 *   "startTime": "10:00",
 *   "endTime": "10:30"
 * }
 * </pre>
 * <p>The system will:</p>
 * <ol>
 *   <li>Create appointment with status BOOKED</li>
 *   <li>Reserve slot (increment booking count)</li>
 *   <li>Save appointment to database</li>
 *   <li>Send confirmation email to customer</li>
 * </ol>
 *
 * @see Appointment
 * @see AppointmentDTO
 * @see AppointmentService
 * @see UpdateSlotStatePort
 * @see AppointmentEventService
 */
@UseCase
@Slf4j
@RequiredArgsConstructor
@Validated
public class BookAppointmentUseCase {

    private final AppointmentService appointmentService;
    private final UpdateSlotStatePort updateSlotStatePort;
    private final AppointmentEventService appointmentEventService;


    @Transactional
    public Appointment execute(@Valid AppointmentDTO appointmentDTO){


        LocalDateTime dateTime = appointmentDTO.day().atTime(appointmentDTO.startTime());
        var appointment = new Appointment(appointmentDTO.slotId(), appointmentDTO.branchId(), appointmentDTO.customerUsername(),
                appointmentDTO.serviceType(), dateTime);

        log.debug("Book appointment created: {}", appointment);

        try {
            updateSlotStatePort.reserve(appointmentDTO.slotId(), LocalDateTime.now());
            appointment= appointmentService.book(appointment);

        }
        catch (IllegalStateException  | IllegalArgumentException e){
            log.error("Illegal state/argument exception. customer username {}", appointmentDTO.customerUsername(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(),e);
        }
        catch (ResponseStatusException e) {

            log.error("Failed to reserve slot: {}", appointmentDTO.slotId(),e);
            throw e;
        }
        catch (EntityAlreadyExistException e) {

            log.debug("Book appointment already exist: {}", appointment,e);
            throw  new ResponseStatusException(HttpStatus.CONFLICT, "User have existing appointment on this day.", e);
        }
        catch (Exception e) {

            log.error("Book appointment failed: {}", appointment, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Book appointment failed", e);
        }

        publishBookedEvent(appointment, appointmentDTO);

        return appointment;

    }
    private void publishBookedEvent(Appointment appointment, AppointmentDTO dto) {

        log.info("Appointment booked: {}", dto);
        appointmentEventService.publishEventBookAppointment(
                appointment.getId(),
                appointment.getReference(),
                appointment.getBranchId(),
                appointment.getCustomerUsername(),
                dto.day(),
                dto.startTime(),
                dto.endTime(),
                LocalDateTime.now());

    }

}
