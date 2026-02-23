package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.AttendingAppointmentStateTransitionAction;
import capitec.branch.appointment.event.app.Topics;
import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.sharekernel.event.metadata.AppointmentMetadata;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.user.app.GetUserQuery;
import capitec.branch.appointment.user.app.dto.UsernameCommand;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.sharekernel.EventToJSONMapper;
import capitec.branch.appointment.sharekernel.EventTrigger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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


    private Appointment bookedAppointment;
    private User customer;
    private String staffUsername;
    private Consumer<String, String> testConsumer;



    @BeforeEach
    void setUpAppointment() {
        Slot slot = slots.getFirst();
        String customerUsername = guestClients.getFirst();
        customer = getUserQuery.execute(new UsernameCommand(customerUsername),UUID.randomUUID().toString());
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
                branch.getBranchId(), slot.getDay(), customer.getUsername()

        ).orElseThrow();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(), "test-group-"+UUID.randomUUID(), "true");

        // Explicitly set deserializers to String
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> cf =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        testConsumer = cf.createConsumer();
        testConsumer.subscribe(List.of(Topics.ATTENDED_APPOINTMENT,Topics.APPOINTMENT_CANCELED));

    }
    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            // Drain remaining messages
            try {
                testConsumer.poll(Duration.ofMillis(100));
            } catch (Exception e) {
                // Ignore
            }
            testConsumer.close();
        }
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

            Appointment updated = appointmentQueryPort.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo( CHECKED_IN);

            // Poll for Kafka event (CORRECT TOPIC)
            Optional<EventValue<String,AppointmentMetadata>> eventValueOptional = getLatestRecordForKey(
                    testConsumer,
                    bookedAppointment.getId().toString()+bookedAppointment.getReference(),
                    Duration.ofSeconds(10)
            );
            assertThat(eventValueOptional).isPresent();

            ObjectMapper mapper = EventToJSONMapper.getMapper();
            var eventValue = eventValueOptional.get();
            AssertionsForClassTypes.assertThat(eventValue).isNotNull();
            AppointmentMetadata metadata = eventValue.value();
            AssertionsForClassTypes.assertThat(metadata).isNotNull();
            assertThat(metadata.id()).isEqualTo(bookedAppointment.getId());
            AssertionsForClassTypes.assertThat(metadata.branchId()).isEqualTo(bookedAppointment.getBranchId());
            AssertionsForClassTypes.assertThat(metadata.customerUsername()).isEqualTo(bookedAppointment.getCustomerUsername());
            assertThat(metadata.createdAt().toLocalDate()).isEqualTo(LocalDate.now());
            Map<String, Object> stringObjectMap = metadata.otherData();
            var triggeredBy =  mapper.convertValue(stringObjectMap.get("triggeredBy"), String.class);
            assertThat(triggeredBy).isNotNull().isEqualTo(EventTrigger.CUSTOMER.name());
            var fromState =  mapper.convertValue(stringObjectMap.get("fromState"), String.class);
            assertThat(fromState).isNotNull().isEqualTo(BOOKED.name());
            var toState =  mapper.convertValue(stringObjectMap.get("toState"), String.class);
            assertThat(toState).isNotNull().isEqualTo(CHECKED_IN.name());
            AssertionsForClassTypes.assertThat(bookedAppointment.getReference()).isEqualTo(metadata.reference());

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
        void shouldStartServiceSuccessfully()  {
            var action = new AttendingAppointmentStateTransitionAction.StartService(
                    bookedAppointment.getId(),
                    staffUsername
            );

            attendAppointmentUseCase.execute(action);

            Appointment updated = appointmentQueryPort.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(IN_PROGRESS);

            // Poll for Kafka event (CORRECT TOPIC)
            Optional<EventValue<String,AppointmentMetadata>> eventValueOptional = getLatestRecordForKey(
                    testConsumer,
                    bookedAppointment.getId().toString()+bookedAppointment.getReference(),
                    Duration.ofSeconds(10)
            );
            assertThat(eventValueOptional).isPresent();
            EventValue<String, AppointmentMetadata> eventValue = eventValueOptional.get();
            AssertionsForClassTypes.assertThat(eventValue).isNotNull();
            AppointmentMetadata metadata = eventValue.value();
            AssertionsForClassTypes.assertThat(metadata).isNotNull();
            Map<String, Object> stringObjectMap = metadata.otherData();
            var triggeredBy =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("triggeredBy"), String.class);
            assertThat(triggeredBy).isNotNull().isEqualTo(EventTrigger.STAFF.name());
            var fromState =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("fromState"), String.class);
            assertThat(fromState).isNotNull().isEqualTo(CHECKED_IN.name());
            var toState =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("toState"), String.class);
            assertThat(toState).isNotNull().isEqualTo(IN_PROGRESS.name());


        }

        @Test
        @DisplayName("Should throw exception when starting service on BOOKED appointment")
        void shouldThrowWhenNotCheckedIn() {
            Slot anotherSlot = slots.get(1);

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
        void shouldCompleteSuccessfully()  {
            var action = new AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(
                    bookedAppointment.getId(),
                    staffUsername
            );

            attendAppointmentUseCase.execute(action);

            Appointment updated = appointmentQueryPort.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);

            // Poll for Kafka event (CORRECT TOPIC)
            Optional<EventValue<String,AppointmentMetadata>> eventValueOptional = getLatestRecordForKey(
                    testConsumer,
                    bookedAppointment.getId().toString()+bookedAppointment.getReference(),
                    Duration.ofSeconds(10)
            );
            assertThat(eventValueOptional).isPresent();
            EventValue<String, AppointmentMetadata> eventValue = eventValueOptional.get();
            AssertionsForClassTypes.assertThat(eventValue).isNotNull();
            AppointmentMetadata metadata = eventValue.value();
            AssertionsForClassTypes.assertThat(metadata).isNotNull();
            Map<String, Object> stringObjectMap = metadata.otherData();
            var triggeredBy =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("triggeredBy"), String.class);
            assertThat(triggeredBy).isNotNull().isEqualTo(EventTrigger.STAFF.name());
            var fromState =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("fromState"), String.class);
            assertThat(fromState).isNotNull().isEqualTo(IN_PROGRESS.name());
            var toState =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("toState"), String.class);
            assertThat(toState).isNotNull().isEqualTo(COMPLETED.name());
        }
    }

    @Nested
    @DisplayName("Cancel By Staff Tests")
    class CancelByStaffTests {

        @Test
        @DisplayName("Should cancel BOOKED appointment by staff")
        void shouldCancelBookedAppointment()  {
            String reason = "Customer requested cancellation via phone";

            var action = new AttendingAppointmentStateTransitionAction.CancelByStaff(
                    staffUsername,
                    reason,
                    bookedAppointment.getId()
            );

            attendAppointmentUseCase.execute(action);

            Appointment updated = appointmentQueryPort.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            // Poll for Kafka event (CORRECT TOPIC)
            Optional<EventValue<String,AppointmentMetadata>> eventValueOptional = getLatestRecordForKey(
                    testConsumer,
                    bookedAppointment.getId().toString()+bookedAppointment.getReference(),
                    Duration.ofSeconds(10)
            );
            assertThat(eventValueOptional).isPresent();
            EventValue<String, AppointmentMetadata> eventValue = eventValueOptional.get();
            AssertionsForClassTypes.assertThat(eventValue).isNotNull();
            AppointmentMetadata metadata = eventValue.value();
            AssertionsForClassTypes.assertThat(metadata).isNotNull();
            Map<String, Object> stringObjectMap = metadata.otherData();
            var triggeredBy =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("triggeredBy"), String.class);
            assertThat(triggeredBy).isNotNull().isEqualTo(EventTrigger.STAFF.name());
            var fromState =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("fromState"), String.class);
            assertThat(fromState).isNotNull().isEqualTo(BOOKED.name());
            var toState =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("toState"), String.class);
            assertThat(toState).isNotNull().isEqualTo(CANCELLED.name());


        }

        @Test
        @DisplayName("Should cancel CHECKED_IN appointment by staff")
        void shouldCancelCheckedInAppointment()  {
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

            Appointment updated = appointmentQueryPort.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);


             updated = appointmentQueryPort.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            // Poll for Kafka event (CORRECT TOPIC)
            Optional<EventValue<String,AppointmentMetadata>> eventValueOptional = getLatestRecordForKey(
                    testConsumer,
                    bookedAppointment.getId().toString()+bookedAppointment.getReference(),
                    Duration.ofSeconds(10)
            );
            assertThat(eventValueOptional).isPresent();
            EventValue<String, AppointmentMetadata> eventValue = eventValueOptional.get();
            AssertionsForClassTypes.assertThat(eventValue).isNotNull();
            AppointmentMetadata metadata = eventValue.value();
            AssertionsForClassTypes.assertThat(metadata).isNotNull();
            Map<String, Object> stringObjectMap = metadata.otherData();
            var triggeredBy =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("triggeredBy"), String.class);
            assertThat(triggeredBy).isNotNull().isEqualTo(EventTrigger.STAFF.name());
            var fromState =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("fromState"), String.class);
            assertThat(fromState).isNotNull().isEqualTo(CHECKED_IN.name());
            var toState =  EventToJSONMapper.getMapper().convertValue(stringObjectMap.get("toState"), String.class);
            assertThat(toState).isNotNull().isEqualTo(CANCELLED.name());
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
            Appointment afterCheckIn = appointmentQueryPort.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(afterCheckIn.getStatus()).isEqualTo(CHECKED_IN);

            // Start service
            attendAppointmentUseCase.execute(
                    new AttendingAppointmentStateTransitionAction.StartService(
                            bookedAppointment.getId(),
                            staffUsername
                    )
            );
            Appointment afterStart = appointmentQueryPort.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(afterStart.getStatus()).isEqualTo(IN_PROGRESS);

            // Complete
            attendAppointmentUseCase.execute(
                    new AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(
                            bookedAppointment.getId(),
                            staffUsername
                    )
            );
            Appointment afterComplete = appointmentQueryPort.findById(bookedAppointment.getId()).orElseThrow();
            assertThat(afterComplete.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        }
    }
}
