package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.CustomerUpdateAppointmentAction;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.user.app.GetUserQuery;
import capitec.branch.appointment.user.app.UsernameCommand;
import capitec.branch.appointment.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static capitec.branch.appointment.appointment.domain.AppointmentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CustomerUpdateAppointmentUseCase Integration Test")
class CustomerUpdateAppointmentUseCaseTest extends AppointmentTestBase {

    @Autowired
    private CustomerUpdateAppointmentUseCase customerUpdateAppointmentUseCase;

    @Autowired
    private BookAppointmentUseCase bookAppointmentUseCase;

    @Autowired
    private GetUserQuery getUserQuery;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointEventListenerTest appointmentEventListenerTest;

    private Appointment bookedAppointment;
    private User customer;
    private Slot slot;

    @BeforeEach
    void setUpAppointment() {
        slot = slots.getFirst();
        Branch branch = branches.getFirst();
        String customerUsername = guestClients.getFirst();
        customer = getUserQuery.execute(new UsernameCommand(customerUsername));

        AppointmentDTO dto = new AppointmentDTO(
                slot.getId(),
                branch.getBranchId(),
                customer.getUsername(),
                "Deposit",
                slot.getDay(),
                slot.getStartTime(),
                slot.getEndTime()
        );

        bookAppointmentUseCase.execute(dto);
        bookedAppointment = appointmentService.getUserActiveAppointment(
                branchId,
                slot.getDay(),
                customer.getUsername()
        ).orElseThrow();
    }

    @Nested
    @DisplayName("Cancel Appointment Tests")
    class CancelAppointmentTests {

        @Test
        @DisplayName("Should cancel appointment successfully and publish event")
        void shouldCancelAppointmentSuccessfully() {
            var action = new CustomerUpdateAppointmentAction.Cancel(
                    bookedAppointment.getId(),
                    customer.getUsername()
            );

            Appointment result = customerUpdateAppointmentUseCase.execute(action);

            assertThat(result.getStatus()).isEqualTo(CANCELLED);

            Appointment updated = appointmentService.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(CANCELLED);
        }

        @Test
        @DisplayName("Should publish CustomerCanceledAppointmentEvent on cancel")
        void shouldPublishCanceledEvent() {
            var action = new CustomerUpdateAppointmentAction.Cancel(
                    bookedAppointment.getId(),
                    customer.getUsername()
            );

            customerUpdateAppointmentUseCase.execute(action);

            CustomerCanceledAppointmentEvent event = appointmentEventListenerTest.canceledEvent;
            assertThat(event).isNotNull();
            assertThat(event.appointmentId()).isEqualTo(bookedAppointment.getId());
            assertThat(event.customerUsername()).isEqualTo(customer.getUsername());
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when appointment does not exist")
        void shouldThrowNotFoundWhenAppointmentNotExists() {
            var action = new CustomerUpdateAppointmentAction.Cancel(
                    UUID.randomUUID(),
                    customer.getUsername()
            );

            assertThatThrownBy(() -> customerUpdateAppointmentUseCase.execute(action))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw exception when cancelling already cancelled appointment")
        void shouldThrowWhenAlreadyCancelled() {
            var action = new CustomerUpdateAppointmentAction.Cancel(
                    bookedAppointment.getId(),
                    customer.getUsername()
            );

            customerUpdateAppointmentUseCase.execute(action);

            assertThatThrownBy(() -> customerUpdateAppointmentUseCase.execute(action))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Reschedule Appointment Tests")
    class RescheduleAppointmentTests {

        @Test
        @DisplayName("Should reschedule appointment successfully")
        void shouldRescheduleAppointmentSuccessfully() {
            Slot newSlot = slots.get(1);
            LocalDateTime newDateTime = LocalDateTime.of(newSlot.getDay(), newSlot.getStartTime());

            var action = new CustomerUpdateAppointmentAction.Reschedule(
                    bookedAppointment.getId(),
                    newSlot.getId(),
                    newDateTime
            );

            Appointment result = customerUpdateAppointmentUseCase.execute(action);

            assertThat(result.getStatus()).isEqualTo(RESCHEDULED);
            assertThat(result.getSlotId()).isEqualTo(newSlot.getId());
        }

        @Test
        @DisplayName("Should publish CustomerRescheduledAppointmentEvent on reschedule")
        void shouldPublishRescheduledEvent() {
            Slot newSlot = slots.get(1);
            LocalDateTime newDateTime = LocalDateTime.of(newSlot.getDay(), newSlot.getStartTime());

            var action = new CustomerUpdateAppointmentAction.Reschedule(
                    bookedAppointment.getId(),
                    newSlot.getId(),
                    newDateTime
            );

            customerUpdateAppointmentUseCase.execute(action);

            CustomerRescheduledAppointmentEvent event = appointmentEventListenerTest.rescheduledEvent;
            assertThat(event).isNotNull();
            assertThat(event.appointmentId()).isEqualTo(bookedAppointment.getId());
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when rescheduling non-existent appointment")
        void shouldThrowNotFoundWhenReschedulingNonExistent() {
            Slot newSlot = slots.get(1);

            var action = new CustomerUpdateAppointmentAction.Reschedule(
                    UUID.randomUUID(),
                    newSlot.getId(),
                    LocalDateTime.now().plusDays(1)
            );

            assertThatThrownBy(() -> customerUpdateAppointmentUseCase.execute(action))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw FORBIDDEN when cancelling another user's appointment")
        void shouldThrowForbiddenWhenCancellingOtherUsersAppointment() {
            String anotherUsername = guestClients.get(1);

            var action = new CustomerUpdateAppointmentAction.Cancel(
                    bookedAppointment.getId(),
                    anotherUsername
            );

            assertThatThrownBy(() -> customerUpdateAppointmentUseCase.execute(action))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
