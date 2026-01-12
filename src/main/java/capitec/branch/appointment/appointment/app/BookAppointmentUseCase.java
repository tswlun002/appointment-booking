package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.port.UpdateSlotStatePort;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@UseCase
@Slf4j
@RequiredArgsConstructor
@Validated
public class BookAppointmentUseCase {

    private final AppointmentService appointmentService;
    private final ApplicationEventPublisher publisher;
    private final UpdateSlotStatePort updateSlotStatePort;


    @Transactional
    boolean execute(@Valid AppointmentDTO appointmentDTO){


        LocalDateTime dateTime = appointmentDTO.day().atTime(appointmentDTO.startTime());
        var appointment = new Appointment(appointmentDTO.slotId(), appointmentDTO.branchId(), appointmentDTO.customerUsername(),
                appointmentDTO.serviceType(), dateTime);

        log.debug("Book appointment created: {}", appointment);

        try {
            updateSlotStatePort.execute(appointmentDTO.slotId(), LocalDateTime.now());
            appointment= appointmentService.book(appointment);

        } catch (ResponseStatusException e) {

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

        return true;

    }
    private void publishBookedEvent(Appointment appointment, AppointmentDTO dto) {
        AppointmentBookedEvent event = new AppointmentBookedEvent(
                appointment.getId(),
                appointment.getReference(),
                appointment.getBranchId(),
                appointment.getCustomerUsername(),
                dto.day(),
                dto.startTime(),
                dto.endTime()
        );
        log.info("Appointment booked: {}", event);
        publisher.publishEvent(event);

    }

}
