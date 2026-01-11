package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.branch.app.GetBranchQuery;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.exeption.SlotFullyBookedException;
import capitec.branch.appointment.slots.app.SlotStatusTransitionAction;
import capitec.branch.appointment.slots.app.UpdateSlotStatusUseCase;
import capitec.branch.appointment.user.app.GetUserQuery;
import capitec.branch.appointment.user.app.UsernameCommand;
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
    private final GetBranchQuery getBranchQuery;
    private final GetUserQuery getUserQuery;
    private final UpdateSlotStatusUseCase updateSlotStatusUseCase;

    @Transactional
    boolean execute(@Valid AppointmentDTO appointmentDTO){


        LocalDateTime dateTime = appointmentDTO.day().atTime(appointmentDTO.startTime());
        var appointment = new Appointment(appointmentDTO.slotId(), appointmentDTO.branchId(), appointmentDTO.customerUsername(),
                appointmentDTO.serviceType(), dateTime);

        log.debug("Book appointment created: {}", appointment);

        updateSlotStatusUseCase.execute(new SlotStatusTransitionAction.Book(appointmentDTO.slotId(), LocalDateTime.now()));

        try {

            appointment= appointmentService.book(appointment);

        }
        catch (SlotFullyBookedException e){

            log.error("Slot is already booked: {}", appointment, e);

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is already booked");
        }
        catch (EntityAlreadyExistException e) {

            log.debug("Book appointment already exist: {}", appointment,e);
            throw  new ResponseStatusException(HttpStatus.CONFLICT, "User have existing appointment on this day.", e);
        }
        catch (Exception e) {

            log.error("Book appointment failed: {}", appointment, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Book appointment failed", e);
        }

        var isAdded = false;
        if (appointment != null) {

            Branch branch = getBranchQuery.execute(appointment.getBranchId());
            var user  = getUserQuery.execute(new UsernameCommand(appointment.getCustomerUsername()));


            AppointmentBookedEvent appointmentBookedEvent = new AppointmentBookedEvent(
                    appointment.getReference(), appointment.getCustomerUsername(), user.getEmail(),
                    appointmentDTO.day(), appointmentDTO.startTime(), appointmentDTO.endTime(), branch.getBranchId(),
                    branch.getAddress()
            );

            log.info("Book appointment successfully created: {}", appointmentBookedEvent);

            publisher.publishEvent(appointmentBookedEvent);
            isAdded = true;
        }

        return isAdded;

    }

}
