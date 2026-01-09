package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.branch.app.GetBranchQuery;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.exeption.SlotIsAlreadyBookedException;
import capitec.branch.appointment.slots.app.GetSlotQuery;
import capitec.branch.appointment.slots.app.SlotStatusTransitionAction;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.user.app.GetUserQuery;
import capitec.branch.appointment.user.app.UsernameCommand;
import capitec.branch.appointment.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@DisplayName("BookAppointmentUseCase Integration Test (Real Dependencies)")
class BookAppointmentUseCaseIntegrationTest extends AppointmentTestBase {

    // --- Real Beans Autowired ---
    @Autowired
    private BookAppointmentUseCase bookAppointmentUseCase;
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private GetBranchQuery getBranchQuery;
    @Autowired
    private GetSlotQuery slotsQuery;
    @Autowired
    private GetUserQuery getUserQuery;

    @Autowired
    private  AppointEventListenerTest  appointmentEventListenerTest;


    // --- Mocked Isolation Point ---
    // The publisher is often mocked to verify event publication without side effects.
    @Autowired
    private ApplicationEventPublisher publisher;

    // --- Test Data ---
    private AppointmentDTO validAppointmentDTO;
    private Appointment mockBookedAppointment;

    @Nested
    @DisplayName("Successful Booking Flow")
    class SuccessfulBooking {

        @Test
        @DisplayName("Should execute successfully and publish event with real beans")
        void shouldExecuteSuccessfullyAndPublishEvent()  {
            Slot slot = slots.getFirst();
            Branch branch = branches.getFirst();
            String customerUsername = guestClients.getFirst();
            User user = getUserQuery.execute(new UsernameCommand(customerUsername));

            String serviceType = "Deposit";

            validAppointmentDTO = new AppointmentDTO(slot.getId(), branchId, user.getUsername(), serviceType);

            // 2. Execute the Use Case
            boolean result = bookAppointmentUseCase.execute(validAppointmentDTO);

            // 3. Assertions
            assertTrue(result, "The execution should return true on successful event publication.");

            AppointmentBookedEvent bookedEvent = appointmentEventListenerTest.bookedEvent;
            assertThat(bookedEvent).isNotNull();
            assertThat(bookedEvent.appointmentReference()).isNotNull();
            assertThat(bookedEvent.day()).isEqualTo(slot.getDay());
            assertThat(bookedEvent.startTime()).isEqualTo(slot.getStartTime());
            assertThat(bookedEvent.endTime()).isEqualTo(slot.getEndTime());
            assertThat(bookedEvent.branchName()).isEqualTo(branch.getBranchId());
            assertThat(bookedEvent.address()).isEqualTo(branch.getAddress());
            assertThat(bookedEvent.username()).isEqualTo(user.getUsername() );
            assertThat(bookedEvent.email()).isEqualTo(user.getEmail());

        }
        @Test
        @DisplayName("Should execute successfully and publish event with real beans")
        void bookOneSlotByManyUserButStillLessThanMaxAllowed_shouldExecuteSuccessfullyAndPublishEvent() {
            // Arrange: Ensure slot is AVAILABLE and save it to establish version=0
            // (This is handled by your @BeforeEach, but we'll confirm the initial version)
            Slot slot = slots.getFirst();
            Branch branch = branches.getFirst();
            String customerUsername = guestClients.getFirst();
            User user = getUserQuery.execute(new UsernameCommand(customerUsername));

            String serviceType = "Deposit";

            validAppointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), user.getUsername(), serviceType);

            // 2. Execute the Use Case
            boolean result = bookAppointmentUseCase.execute(validAppointmentDTO);
            assertThat(result).isTrue();
            AppointmentBookedEvent bookedEvent = appointmentEventListenerTest.bookedEvent;
            assertThat(bookedEvent).isNotNull();
            assertThat(bookedEvent.appointmentReference()).isNotNull();
            assertThat(bookedEvent.day()).isEqualTo(slot.getDay());
            assertThat(bookedEvent.startTime()).isEqualTo(slot.getStartTime());
            assertThat(bookedEvent.endTime()).isEqualTo(slot.getEndTime());
            assertThat(bookedEvent.branchName()).isEqualTo(branch.getBranchId());
            assertThat(bookedEvent.address()).isEqualTo(branch.getAddress());
            assertThat(bookedEvent.username()).isEqualTo(user.getUsername() );
            assertThat(bookedEvent.email()).isEqualTo(user.getEmail());

            // Another user pick same slot
            AppointmentDTO appointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), guestClients.get(1), serviceType);
            result = bookAppointmentUseCase.execute(appointmentDTO);
            user = getUserQuery.execute(new UsernameCommand(guestClients.get(1)));
            assertThat(result).isTrue();
            bookedEvent = appointmentEventListenerTest.bookedEvent;
            assertThat(bookedEvent).isNotNull();
            assertThat(bookedEvent.appointmentReference()).isNotNull();
            assertThat(bookedEvent.day()).isEqualTo(slot.getDay());
            assertThat(bookedEvent.startTime()).isEqualTo(slot.getStartTime());
            assertThat(bookedEvent.endTime()).isEqualTo(slot.getEndTime());
            assertThat(bookedEvent.branchName()).isEqualTo(branch.getBranchId());
            assertThat(bookedEvent.address()).isEqualTo(branch.getAddress());
            assertThat(bookedEvent.username()).isEqualTo(user.getUsername() );
            assertThat(bookedEvent.email()).isEqualTo(user.getEmail());


        }
    }

    @Nested
    @DisplayName("Failure Cases (Exception Handling)")
    class FailureCases {

        // We still need to mock the service's exception behavior to test the UseCase's error handling.

        @Test
        @DisplayName("Should throw CONFLICT (409) when SlotIsAlreadyBookedException occurs")
        void shouldThrowConflictWhenSlotIsAlreadyBooked() throws Exception {
            // Arrange: Ensure slot is AVAILABLE and save it to establish version=0
            // (This is handled by your @BeforeEach, but we'll confirm the initial version)
            Slot slot = slots.getFirst();
            Branch branch = branches.getFirst();
            String customerUsername = guestClients.getFirst();
            User user = getUserQuery.execute(new UsernameCommand(customerUsername));

            String serviceType = "Deposit";

            validAppointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), user.getUsername(), serviceType);

            // 2. Execute the Use Case
            boolean result = bookAppointmentUseCase.execute(validAppointmentDTO);
            assertThat(result).isTrue();
            Appointment appointment = appointmentService.branchAppointments(branch.getBranchId()).stream().toList().getFirst();
            long initialVersion = appointment.getVersion();

            AppointmentDTO appointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), guestClients.get(1), serviceType);
            boolean booked = bookAppointmentUseCase.execute(appointmentDTO);
            assertThat(booked).isFalse();


            }

        @Test
        @DisplayName("Should throw CONFLICT (409) when EntityAlreadyExistException occurs")
        void shouldThrowConflictWhenEntityAlreadyExist() throws Exception {
            when(appointmentService.book(any(Appointment.class)))
                    .thenThrow(new EntityAlreadyExistException("User has existing appointment"));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                    bookAppointmentUseCase.execute(validAppointmentDTO)
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw INTERNAL_SERVER_ERROR (500) for unhandled exceptions")
        void shouldThrowInternalServerErrorForUnhandledExceptions() throws Exception {
            when(appointmentService.book(any(Appointment.class)))
                    .thenThrow(new RuntimeException("Simulated unexpected failure"));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                    bookAppointmentUseCase.execute(validAppointmentDTO)
            );

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            verify(publisher, never()).publishEvent(any());
        }
    }
}