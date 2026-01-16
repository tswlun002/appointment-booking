package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.AttendingAppointmentStateTransitionAction;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.appointment.app.dto.AppointmentStateChangedEvent;
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

import java.time.LocalDate;
import java.util.UUID;

import static capitec.branch.appointment.appointment.domain.AppointmentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AttendAppointmentUseCase Integration Test")
class AttendAppointmentUseCaseTest extends AppointmentTestBase {

    @Autowired
    private AttendAppointmentUseCase attendAppointmentUseCase;

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
    private String staffUsername;

    @BeforeEach
    void setUpAppointment() {
        Slot slot = slots.getFirst();
        Branch branch = branches.getFirst();
        String customerUsername = guestClients.getFirst();
        customer = getUserQuery.execute(new UsernameCommand(customerUsername));
        staffUsername = staff.getFirst();

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
    @DisplayName("Check-In Tests")
    class CheckInTests {

        @Test
        @DisplayName("Should check in successfully and publish event")
        void shouldCheckInSuccessfully() {
            var action = new AttendingAppointmentStateTransitionAction.CheckIn(
                    bookedAppointment.getBranchId(),
                    bookedAppointment.getDateTime().toLocalDate(),
                    customer.getUsername()
            );

            attendAppointmentUseCase.execute(action);

            Appointment updated = appointmentService.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(CHECKED_IN);

            AppointmentStateChangedEvent event = appointmentEventListenerTest.bookedEvent;
            assertThat(event).isNotNull();
            assertThat(event.appointmentId()).isEqualTo(bookedAppointment.getId());
            assertThat(event.fromState()).isEqualTo(BOOKED);
            assertThat(event.toState()).isEqualTo(CHECKED_IN);
            assertThat(event.triggeredBy()).isEqualTo(customer.getUsername());
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when appointment does not exist")
        void shouldThrowNotFoundWhenAppointmentNotExists() {
            var action = new AttendingAppointmentStateTransitionAction.CheckIn(
                    UUID.randomUUID().toString(),
                    LocalDate.now(),
                    customer.getUsername()
            );

            assertThatThrownBy(() -> attendAppointmentUseCase.execute(action))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw exception when checking in already checked-in appointment")
        void shouldThrowWhenAlreadyCheckedIn() {
            var action = new AttendingAppointmentStateTransitionAction.CheckIn(
                    bookedAppointment.getBranchId(),
                    bookedAppointment.getDateTime().toLocalDate(),
                    customer.getUsername()
            );

            attendAppointmentUseCase.execute(action);

            assertThatThrownBy(() -> attendAppointmentUseCase.execute(action))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Start Service Tests")
    class StartServiceTests {

        @BeforeEach
        void checkInFirst() {
            var checkIn = new AttendingAppointmentStateTransitionAction.CheckIn(
                    bookedAppointment.getBranchId(),
                    bookedAppointment.getDateTime().toLocalDate(),
                    customer.getUsername()
            );
            attendAppointmentUseCase.execute(checkIn);
        }

        @Test
        @DisplayName("Should start service successfully and publish event")
        void shouldStartServiceSuccessfully() {
            var action = new AttendingAppointmentStateTransitionAction.StartService(
                    bookedAppointment.getId(),
                    staffUsername
            );

            attendAppointmentUseCase.execute(action);

            Appointment updated = appointmentService.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(IN_PROGRESS);

            AppointmentStateChangedEvent event = appointmentEventListenerTest.bookedEvent;
            assertThat(event).isNotNull();
            assertThat(event.fromState()).isEqualTo(CHECKED_IN);
            assertThat(event.toState()).isEqualTo(IN_PROGRESS);
            assertThat(event.triggeredBy()).isEqualTo(staffUsername);
            assertThat(event.metadata()).containsEntry("staffUsername", staffUsername);
        }

        @Test
        @DisplayName("Should throw exception when starting service on BOOKED appointment")
        void shouldThrowWhenNotCheckedIn() {
            Slot anotherSlot = slots.get(1);
            Branch branch = branches.getFirst();
            String anotherCustomer = guestClients.get(1);

            AppointmentDTO dto = new AppointmentDTO(
                    anotherSlot.getId(),
                    branch.getBranchId(),
                    anotherCustomer,
                    "Deposit",
                    anotherSlot.getDay(),
                    anotherSlot.getStartTime(),
                    anotherSlot.getEndTime()
            );
            bookAppointmentUseCase.execute(dto);

            Appointment notCheckedIn = appointmentService.getUserActiveAppointment(
                    branch.getBranchId(),
                    anotherSlot.getDay(),
                    anotherCustomer
            ).orElseThrow();

            var action = new AttendingAppointmentStateTransitionAction.StartService(
                    notCheckedIn.getId(),
                    staffUsername
            );

            assertThatThrownBy(() -> attendAppointmentUseCase.execute(action))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Complete Appointment Tests")
    class CompleteAppointmentTests {

        @BeforeEach
        void startServiceFirst() {
            var checkIn = new AttendingAppointmentStateTransitionAction.CheckIn(
                    bookedAppointment.getBranchId(),
                    bookedAppointment.getDateTime().toLocalDate(),
                    customer.getUsername()
            );
            attendAppointmentUseCase.execute(checkIn);

            var startService = new AttendingAppointmentStateTransitionAction.StartService(
                    bookedAppointment.getId(),
                    staffUsername
            );
            attendAppointmentUseCase.execute(startService);
        }

        @Test
        @DisplayName("Should complete appointment successfully and publish event")
        void shouldCompleteSuccessfully() {
            var action = new AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(
                    bookedAppointment.getId(),
                    staffUsername
            );

            attendAppointmentUseCase.execute(action);

            Appointment updated = appointmentService.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);

            AppointmentStateChangedEvent event = appointmentEventListenerTest.bookedEvent;
            assertThat(event).isNotNull();
            assertThat(event.fromState()).isEqualTo(IN_PROGRESS);
            assertThat(event.toState()).isEqualTo(COMPLETED);
            assertThat(event.triggeredBy()).isEqualTo("system");
        }
    }

    @Nested
    @DisplayName("Cancel By Staff Tests")
    class CancelByStaffTests {

        @Test
        @DisplayName("Should cancel BOOKED appointment by staff")
        void shouldCancelBookedAppointment() {
            String reason = "Customer requested cancellation via phone";

            var action = new AttendingAppointmentStateTransitionAction.CancelByStaff(
                    staffUsername,
                    reason,
                    bookedAppointment.getId()
            );

            attendAppointmentUseCase.execute(action);

            Appointment updated = appointmentService.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);

            AppointmentStateChangedEvent event = appointmentEventListenerTest.bookedEvent;
            assertThat(event).isNotNull();
            assertThat(event.fromState()).isEqualTo(BOOKED);
            assertThat(event.toState()).isEqualTo(CANCELLED);
            assertThat(event.triggeredBy()).isEqualTo(staffUsername);
            assertThat(event.metadata()).containsEntry("reason", reason);
        }

        @Test
        @DisplayName("Should cancel CHECKED_IN appointment by staff")
        void shouldCancelCheckedInAppointment() {
            var checkIn = new AttendingAppointmentStateTransitionAction.CheckIn(
                    bookedAppointment.getBranchId(),
                    bookedAppointment.getDateTime().toLocalDate(),
                    customer.getUsername()
            );
            attendAppointmentUseCase.execute(checkIn);

            String reason = "Branch emergency closure";

            var action = new AttendingAppointmentStateTransitionAction.CancelByStaff(
                    staffUsername,
                    reason,
                    bookedAppointment.getId()
            );

            attendAppointmentUseCase.execute(action);

            Appointment updated = appointmentService.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);

            AppointmentStateChangedEvent event = appointmentEventListenerTest.bookedEvent;
            assertThat(event.fromState()).isEqualTo(CHECKED_IN);
            assertThat(event.toState()).isEqualTo(CANCELLED);
        }

        @Test
        @DisplayName("Should throw exception when cancelling COMPLETED appointment")
        void shouldThrowWhenCancellingCompleted() {
            // Complete the appointment first
            var checkIn = new AttendingAppointmentStateTransitionAction.CheckIn(
                    bookedAppointment.getBranchId(),
                    bookedAppointment.getDateTime().toLocalDate(),
                    customer.getUsername()
            );
            attendAppointmentUseCase.execute(checkIn);

            var startService = new AttendingAppointmentStateTransitionAction.StartService(
                    bookedAppointment.getId(),
                    staffUsername
            );
            attendAppointmentUseCase.execute(startService);

            var complete = new AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(
                    bookedAppointment.getId(),
                    staffUsername
            );
            attendAppointmentUseCase.execute(complete);

            var cancelAction = new AttendingAppointmentStateTransitionAction.CancelByStaff(
                    staffUsername,
                    "Test reason",
                    bookedAppointment.getId()
            );

            assertThatThrownBy(() -> attendAppointmentUseCase.execute(cancelAction))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should throw exception when reason is blank")
        void shouldThrowWhenReasonIsBlank() {
            var action = new AttendingAppointmentStateTransitionAction.CancelByStaff(
                    staffUsername,
                    "",
                    bookedAppointment.getId()
            );

            assertThatThrownBy(() -> attendAppointmentUseCase.execute(action))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Full Lifecycle Test")
    class FullLifecycleTests {

        @Test
        @DisplayName("Should complete full appointment lifecycle: BOOKED → CHECKED_IN → IN_PROGRESS → COMPLETED")
        void shouldCompleteFullLifecycle() {
            // Initial state
            assertThat(bookedAppointment.getStatus()).isEqualTo(BOOKED);

            // Check-in
            attendAppointmentUseCase.execute(
                    new AttendingAppointmentStateTransitionAction.CheckIn(
                            bookedAppointment.getBranchId(),
                            bookedAppointment.getDateTime().toLocalDate(),
                            customer.getUsername()
                    )
            );
            Appointment afterCheckIn = appointmentService.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(afterCheckIn.getStatus()).isEqualTo(CHECKED_IN);

            // Start service
            attendAppointmentUseCase.execute(
                    new AttendingAppointmentStateTransitionAction.StartService(
                            bookedAppointment.getId(),
                            staffUsername
                    )
            );
            Appointment afterStart = appointmentService.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(afterStart.getStatus()).isEqualTo(IN_PROGRESS);

            // Complete
            attendAppointmentUseCase.execute(
                    new AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(
                            bookedAppointment.getId(),
                            staffUsername
                    )
            );
            Appointment afterComplete = appointmentService.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(afterComplete.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        }
    }
}
