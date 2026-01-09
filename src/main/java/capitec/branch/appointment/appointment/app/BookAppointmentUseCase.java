package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.branch.app.GetBranchQuery;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.exeption.SlotIsAlreadyBookedException;
import capitec.branch.appointment.slots.app.GetSlotQuery;
import capitec.branch.appointment.user.app.GetUserQuery;
import capitec.branch.appointment.user.app.UsernameCommand;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@UseCase
@Slf4j
@RequiredArgsConstructor
@Validated
public class BookAppointmentUseCase {

    private final AppointmentService appointmentService;
    private final ApplicationEventPublisher publisher;
    private final GetBranchQuery getBranchQuery;
    private final GetSlotQuery slotsQuery;
    private final GetUserQuery getUserQuery;

    boolean execute(@Valid AppointmentDTO appointmentDTO){

        Appointment appointment = new Appointment(appointmentDTO.slotId(), appointmentDTO.branchId(),
                appointmentDTO.customerUsername(), appointmentDTO.serviceType());

        log.debug("Book appointment created: {}", appointment);


        try {

            appointment= appointmentService.book(appointment);

        }
        catch (SlotIsAlreadyBookedException e){

            log.error("Slot is already booked: {}", appointment, e);

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is already booked");
        }
        catch (EntityAlreadyExistException e) {

            log.debug("Book appointment already exist: {}", appointment,e);
            throw  new ResponseStatusException(HttpStatus.CONFLICT, "User cannot book more than one slot per day.", e);
        }
        catch (Exception e) {

            log.error("Book appointment failed: {}", appointment, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Book appointment failed", e);
        }

        var isAdded = false;
        if (appointment != null) {

            Branch branch = getBranchQuery.execute(appointment.getBranchId());
            var slot = slotsQuery.execute(appointment.getSlotId());
            var user  = getUserQuery.execute(new UsernameCommand(appointment.getCustomerUsername()));


            AppointmentBookedEvent appointmentBookedEvent = new AppointmentBookedEvent(
                    appointment.getBookingReference(), appointment.getCustomerUsername(), user.getEmail(),
                    slot.getDay(), slot.getStartTime(), slot.getEndTime(), branch.getBranchId(),
                    branch.getAddress()
            );

            log.info("Book appointment successfully created: {}", appointmentBookedEvent);

            publisher.publishEvent(appointmentBookedEvent);
            isAdded = true;
        }

        return isAdded;

    }
}
