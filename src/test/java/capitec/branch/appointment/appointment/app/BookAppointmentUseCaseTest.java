package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.slots.app.GetSlotQuery;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import capitec.branch.appointment.user.app.GetUserQuery;
import capitec.branch.appointment.user.app.UsernameCommand;
import capitec.branch.appointment.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;


@DisplayName("BookAppointmentUseCase Integration Test (Real Dependencies)")
class BookAppointmentUseCaseTest extends AppointmentTestBase {

    @Autowired
    private BookAppointmentUseCase bookAppointmentUseCase;
    @Autowired
    private GetUserQuery getUserQuery;
    @Autowired
    private AppointEventListenerTest appointmentEventListenerTest;
    private AppointmentDTO validAppointmentDTO;
    @Autowired
    private GetSlotQuery getSlotQuery;


    @Test
    @DisplayName("Should execute successfully and publish event with real beans")
    void shouldExecuteSuccessfullyAndPublishEvent() {

        List<Slot> ListOneSlotADay = slots.stream().collect(Collectors.groupingBy(Slot::getDay)).values().stream().map(List::getFirst).toList();
        for (Slot slot : ListOneSlotADay) {


            Branch branch = branches.getFirst();
            String customerUsername = guestClients.getFirst();
            User user = getUserQuery.execute(new UsernameCommand(customerUsername));

            String serviceType = "Deposit";

            validAppointmentDTO = new AppointmentDTO(slot.getId(), branchId, user.getUsername(), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());

            // 2. Execute the Use Case
            boolean result = bookAppointmentUseCase.execute(validAppointmentDTO);

            // 3. Assertions
            assertTrue(result, "The execution should return true on successful event publication.");

            var bookedEvent = appointmentEventListenerTest.bookedEvent2;
            assertThat(bookedEvent).isNotNull();
            assertThat(bookedEvent.reference()).isNotNull();
            assertThat(bookedEvent.day()).isEqualTo(slot.getDay());
            assertThat(bookedEvent.startTime()).isEqualTo(slot.getStartTime());
            assertThat(bookedEvent.endTime()).isEqualTo(slot.getEndTime());
            assertThat(bookedEvent.branchId()).isEqualTo(branch.getBranchId());
            assertThat(bookedEvent.customerUsername()).isEqualTo(user.getUsername());

            //VERIFY slot booking
            Slot bookedSlot = getSlotQuery.execute(slot.getId());
            assertThat(bookedSlot).isNotNull();
            assertThat(bookedSlot.getBookingCount()).isEqualTo(1);
        }

    }

    @Test
    @DisplayName("Should execute successfully and publish event")
    void bookByMultipleUser_shouldExecuteSuccessfullyAndPublishEvent() {

        Slot slot = slots.getFirst();
        Branch branch = branches.getFirst();
        String customerUsername = guestClients.getFirst();
        User user = getUserQuery.execute(new UsernameCommand(customerUsername));


        String serviceType = "Deposit";

        validAppointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), user.getUsername(), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());

        // 2. Execute the Use Case
        boolean result = bookAppointmentUseCase.execute(validAppointmentDTO);
        assertThat(result).isTrue();
        var bookedEvent = appointmentEventListenerTest.bookedEvent2;
        assertThat(bookedEvent).isNotNull();
        assertThat(bookedEvent.reference()).isNotNull();
        assertThat(bookedEvent.day()).isEqualTo(slot.getDay());
        assertThat(bookedEvent.startTime()).isEqualTo(slot.getStartTime());
        assertThat(bookedEvent.endTime()).isEqualTo(slot.getEndTime());
        assertThat(bookedEvent.branchId()).isEqualTo(branch.getBranchId());
        assertThat(bookedEvent.customerUsername()).isEqualTo(user.getUsername());


        // Another user pick same slot
        AppointmentDTO appointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), guestClients.get(1), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());
        result = bookAppointmentUseCase.execute(appointmentDTO);
        user = getUserQuery.execute(new UsernameCommand(guestClients.get(1)));
        assertThat(result).isTrue();
         bookedEvent = appointmentEventListenerTest.bookedEvent2;
        assertThat(bookedEvent).isNotNull();
        assertThat(bookedEvent.reference()).isNotNull();
        assertThat(bookedEvent.day()).isEqualTo(slot.getDay());
        assertThat(bookedEvent.startTime()).isEqualTo(slot.getStartTime());
        assertThat(bookedEvent.endTime()).isEqualTo(slot.getEndTime());
        assertThat(bookedEvent.branchId()).isEqualTo(branch.getBranchId());
        assertThat(bookedEvent.customerUsername()).isEqualTo(user.getUsername());

        //VERIFY slot booking
        Slot bookedSlot = getSlotQuery.execute(slot.getId());
        assertThat(bookedSlot).isNotNull();
        assertThat(bookedSlot.getBookingCount()).isEqualTo(MAX_BOOKING_CAPACITY);
        assertThat(bookedSlot.getStatus()).isEqualTo(SlotStatus.FULLY_BOOKED);

    }


    @Test
    @DisplayName("Should throw CONFLICT (409) when SlotFullyBookedException occurs")
    void shouldThrowConflictWhenSlotIsFullyBooked() {

        Slot slot = slots.getFirst();
        Branch branch = branches.getFirst();
        String customerUsername = guestClients.getFirst();
        User user = getUserQuery.execute(new UsernameCommand(customerUsername));


        String serviceType = "Deposit";


        // 1. Execute the Use Case
        validAppointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), user.getUsername(), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());
        boolean result = bookAppointmentUseCase.execute(validAppointmentDTO);
        assertThat(result).isTrue();
        var bookedEvent = appointmentEventListenerTest.bookedEvent;
        assertThat(bookedEvent).isNotNull();

        // 2. Execute the Use Case
        validAppointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), guestClients.get(1), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());
        result = bookAppointmentUseCase.execute(validAppointmentDTO);
        assertThat(result).isTrue();
        bookedEvent = appointmentEventListenerTest.bookedEvent;
        assertThat(bookedEvent).isNotNull();

        // 3. Execute the Use Case
        validAppointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), guestClients.get(2), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());
        ResponseStatusException actual = assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> bookAppointmentUseCase.execute(validAppointmentDTO)).actual();
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(actual.getReason()).isEqualTo("Slot is fully booked.");


        //VERIFY slot booking
        Slot bookedSlot = getSlotQuery.execute(slot.getId());
        assertThat(bookedSlot).isNotNull();
        assertThat(bookedSlot.getBookingCount()).isEqualTo(MAX_BOOKING_CAPACITY);
        assertThat(bookedSlot.getStatus()).isEqualTo(SlotStatus.FULLY_BOOKED);


    }

    @Test
    @DisplayName("Should throw CONFLICT (409) when EntityAlreadyExistException occurs")
    void shouldThrowConflict_whenUserHasAppointment_onTheDay() {

        Slot slot = slots.getFirst();
        Branch branch = branches.getFirst();
        String customerUsername = guestClients.getFirst();
        User user = getUserQuery.execute(new UsernameCommand(customerUsername));

        String serviceType = "Deposit";

        // 1. Execute the Use Case
        validAppointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), user.getUsername(), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());
        boolean result = bookAppointmentUseCase.execute(validAppointmentDTO);
        assertThat(result).isTrue();
        var bookedEvent = appointmentEventListenerTest.bookedEvent;
        assertThat(bookedEvent).isNotNull();

        // 2. Execute the Use Case
        Slot secondSlotToBookAtSameDay = slots.get(1);
        validAppointmentDTO = new AppointmentDTO(secondSlotToBookAtSameDay.getId(), branch.getBranchId(), user.getUsername(), serviceType,secondSlotToBookAtSameDay.getDay(),secondSlotToBookAtSameDay.getStartTime(),secondSlotToBookAtSameDay.getEndTime());
        ResponseStatusException actual = assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> bookAppointmentUseCase.execute(validAppointmentDTO)).actual();
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(actual.getReason()).isEqualTo("User have existing appointment on this day.");
        var secondSlotToBookAtSameDay1 = slotService.getSlot(secondSlotToBookAtSameDay.getId()).get();
        //VERIFY THE SLOT TRANSACTION WAS ROLLBACK WHEN SECOND APPOINTMENT FAILED
        assertThat(secondSlotToBookAtSameDay1).isEqualTo(secondSlotToBookAtSameDay);

    }


}