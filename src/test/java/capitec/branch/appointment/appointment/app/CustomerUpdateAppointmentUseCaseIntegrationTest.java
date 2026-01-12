package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.port.UpdateSlotStatePort;
import capitec.branch.appointment.appointment.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@RecordApplicationEvents
@Transactional
class CustomerUpdateAppointmentUseCaseIntegrationTest {

    @Autowired
    private CustomerUpdateAppointmentUseCase useCase;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private TestUpdateSlotStatePort testSlotStatePort;

    @Autowired
    private ApplicationEvents applicationEvents;

    private UUID branchId;
    private UUID slotId;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public TestUpdateSlotStatePort testUpdateSlotStatePort() {
            return new TestUpdateSlotStatePort();
        }
    }

    static class TestUpdateSlotStatePort implements UpdateSlotStatePort {
        private final List<UUID> releasedSlots = new ArrayList<>();

        @Override
        public void release(UUID slotId, LocalDateTime timestamp) {
            releasedSlots.add(slotId);
        }

        public List<UUID> getReleasedSlots() {
            return releasedSlots;
        }

        public void clear() {
            releasedSlots.clear();
        }
    }

    @BeforeEach
    void setUp() {
        branchId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        testSlotStatePort.clear();
    }

    @Nested
    class CancelAppointment {

        @Test
        void shouldCancelAppointmentAndReleaseSlot() {
            Appointment appointment = createAndSaveAppointment();

            var action = new CustomerUpdateAppointmentAction.Cancel(appointment.getId());

            Appointment result = useCase.execute(action);

            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(testSlotStatePort.getReleasedSlots()).contains(slotId);
        }

        @Test
        void shouldPublishCanceledEventOnCancel() {
            Appointment appointment = createAndSaveAppointment();

            var action = new CustomerUpdateAppointmentAction.Cancel(appointment.getId());

            useCase.execute(action);

            long eventCount = applicationEvents.stream(CustomerCanceledAppointmentEvent.class).count();
            assertThat(eventCount).isEqualTo(1);

            CustomerCanceledAppointmentEvent event = applicationEvents
                    .stream(CustomerCanceledAppointmentEvent.class)
                    .findFirst()
                    .orElseThrow();

            assertThat(event.appointmentId()).isEqualTo(appointment.getId());
            assertThat(event.customerUsername()).isEqualTo("testuser");
            assertThat(event.newStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }
    }

    @Nested
    class RescheduleAppointment {

        @Test
        void shouldRescheduleAppointmentAndReleaseNewSlot() {
            Appointment appointment = createAndSaveAppointment();
            UUID newSlotId = UUID.randomUUID();
            LocalDateTime newDateTime = LocalDateTime.now().plusDays(1);

            var action = new CustomerUpdateAppointmentAction.Reschedule(
                    appointment.getId(), newSlotId, newDateTime);

            Appointment result = useCase.execute(action);

            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
            assertThat(testSlotStatePort.getReleasedSlots()).contains(newSlotId);
        }

        @Test
        void shouldPublishRescheduledEventOnReschedule() {
            Appointment appointment = createAndSaveAppointment();
            UUID newSlotId = UUID.randomUUID();

            var action = new CustomerUpdateAppointmentAction.Reschedule(
                    appointment.getId(), newSlotId, LocalDateTime.now().plusDays(1));

            useCase.execute(action);

            long eventCount = applicationEvents.stream(CustomerRescheduledAppointmentEvent.class).count();
            assertThat(eventCount).isEqualTo(1);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldThrowNotFoundWhenAppointmentDoesNotExist() {
            UUID nonExistentId = UUID.randomUUID();
            var action = new CustomerUpdateAppointmentAction.Cancel(nonExistentId);

            assertThatThrownBy(() -> useCase.execute(action))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Appointment not found");
        }
    }

    private Appointment createAndSaveAppointment() {
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .reference("REF-" + UUID.randomUUID().toString().substring(0, 8))
                .customerUsername("testuser")
                .branchId(branchId)
                .slotId(slotId)
                .status(AppointmentStatus.BOOKED)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .build();

        return appointmentService.save(appointment);
    }
}
