package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.CustomerUpdateAppointmentAction;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.event.app.Topics;
import capitec.branch.appointment.event.infrastructure.kafka.producer.appointment.AppointmentEventValueImpl;
import capitec.branch.appointment.kafka.appointment.AppointmentMetadata;
import capitec.branch.appointment.kafka.domain.ExtendedEventValue;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.slots.domain.SlotStatus;
import capitec.branch.appointment.user.app.GetUserQuery;
import capitec.branch.appointment.user.app.UsernameCommand;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.utils.sharekernel.EventToJSONMapper;
import capitec.branch.appointment.utils.sharekernel.EventTrigger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

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
    private SlotService slotService;

    private Appointment bookedAppointment;
    private User customer;
    private Slot slot;
    private Consumer<String, String> testConsumer;

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

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(), "test-group", "true");

        // Explicitly set deserializers to String
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> cf =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        testConsumer = cf.createConsumer();
        testConsumer.subscribe(List.of(Topics.APPOINTMENT_RESCHEDULED,Topics.APPOINTMENT_CANCELED));
    }
    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
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

            Slot slotBefore = slotService.getSlot(bookedAppointment.getSlotId()).orElseThrow();

            Appointment result = customerUpdateAppointmentUseCase.execute(action);

            assertThat(result.getStatus()).isEqualTo(CANCELLED);

            //VERIFY slot is released
            Slot slotAfter = slotService.getSlot(result.getSlotId()).orElseThrow();
            assertThat(slotAfter.getId()).isEqualTo(slotBefore.getId());
            assertThat(slotAfter.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
            assertThat(slotAfter.getBookingCount()).isEqualTo(slotBefore.getBookingCount()-1);

        }

        @Test
        @DisplayName("Should publish CustomerCanceledAppointmentEvent on cancel")
        void shouldPublishCanceledEvent() throws JsonProcessingException {
            var action = new CustomerUpdateAppointmentAction.Cancel(
                    bookedAppointment.getId(),
                    customer.getUsername()
            );

            customerUpdateAppointmentUseCase.execute(action);

//
            // Poll for Kafka event (CORRECT TOPIC)
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(
                    testConsumer,
                    Duration.ofSeconds(10)
            );
            ConsumerRecord<String, String> received = StreamSupport.stream(records.spliterator(), false)
                    .reduce((first, second) ->
                            second.key().equals(bookedAppointment.getId().toString()) &&second.timestamp()>first.timestamp()?second:first)
                    .orElseThrow(() -> new AssertionError("No records found"));

            // Assertions on Kafka event
            AssertionsForClassTypes.assertThat(received).isNotNull();
            AssertionsForClassTypes.assertThat(received.key()).isNotNull();
            String value = received.value();
            AssertionsForClassTypes.assertThat(value).isNotNull();

            ObjectMapper mapper = EventToJSONMapper.getMapper();

            ExtendedEventValue<AppointmentMetadata> extendedEventValue = mapper.readValue(value, AppointmentEventValueImpl.class);
            AssertionsForClassTypes.assertThat(extendedEventValue).isNotNull();
            AppointmentMetadata metadata = extendedEventValue.getMetadata();
            AssertionsForClassTypes.assertThat(metadata).isNotNull();
            assertThat(metadata.id()).isEqualTo(bookedAppointment.getId());
            AssertionsForClassTypes.assertThat(metadata.branchId()).isEqualTo(bookedAppointment.getBranchId());
            AssertionsForClassTypes.assertThat(metadata.customerUsername()).isEqualTo(bookedAppointment.getCustomerUsername());
            assertThat(metadata.createdAt().toLocalDate()).isEqualTo(LocalDate.now());
            Map<String, Object> stringObjectMap = metadata.otherData();
            var triggeredBy =  mapper.convertValue(stringObjectMap.get("triggerBy"), String.class);
            assertThat(triggeredBy).isNotNull().isEqualTo(EventTrigger.CUSTOMER.name());
            var fromState =  mapper.convertValue(stringObjectMap.get("fromState"), String.class);
            assertThat(fromState).isNotNull().isEqualTo(BOOKED.name());
            var toState =  mapper.convertValue(stringObjectMap.get("toState"), String.class);
            assertThat(toState).isNotNull().isEqualTo(CANCELLED.name());
            AssertionsForClassTypes.assertThat(bookedAppointment.getReference()).isEqualTo(metadata.reference());
            AssertionsForClassTypes.assertThat(extendedEventValue.getSource()).isNotNull().isEqualTo("Appointment context");

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
                    .isInstanceOf(ResponseStatusException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Reschedule Appointment Tests")
    class RescheduleAppointmentTests {

        @Test
        @DisplayName("Should reschedule appointment successfully")
        void shouldRescheduleAppointmentSameDay_Successfully() {

            Slot oldSlot = slotService.getSlot(bookedAppointment.getSlotId()).orElseThrow();

            Slot newSlot = slots.get(1);
            LocalDateTime newDateTime = LocalDateTime.of(newSlot.getDay(), newSlot.getStartTime());

            var action = new CustomerUpdateAppointmentAction.Reschedule(
                    bookedAppointment.getId(),
                    newSlot.getId(),
                    newDateTime,
                    newSlot.getEndTime()
            );

            Appointment result = customerUpdateAppointmentUseCase.execute(action);

            assertThat(result.getStatus()).isEqualTo(BOOKED);
            assertThat(result.getSlotId()).isEqualTo(newSlot.getId());

            //VERIFY slot is released
            Slot slotAfter = slotService.getSlot(result.getSlotId()).orElseThrow();
            // verify new slot
            assertThat(slotAfter.getId()).isEqualTo(newSlot.getId());
            assertThat(slotAfter.getBookingCount()).isEqualTo(newSlot.getBookingCount()+1);
            // verify old slot
            Slot oldSlotAfterReschedule = slotService.getSlot(bookedAppointment.getSlotId()).orElseThrow();
            assertThat(oldSlot.getId()).isEqualTo(oldSlotAfterReschedule.getId())
                    .isNotEqualTo(slotAfter.getId()).isNotEqualTo(newSlot.getId());

            assertThat(oldSlot.getBookingCount()-1).isEqualTo(oldSlotAfterReschedule.getBookingCount());
            assertThat(oldSlotAfterReschedule.getStatus()).isEqualTo(SlotStatus.AVAILABLE);



        }
        @Test
        @DisplayName("Should reschedule appointment successfully")
        void shouldRescheduleAppointmentOtherDay_Successfully() {

            Slot oldSlot = slotService.getSlot(bookedAppointment.getSlotId()).orElseThrow();

            Slot newSlot = slots.get(2);
            LocalDateTime newDateTime = LocalDateTime.of(newSlot.getDay(), newSlot.getStartTime());

            var action = new CustomerUpdateAppointmentAction.Reschedule(
                    bookedAppointment.getId(),
                    newSlot.getId(),
                    newDateTime,
                    newSlot.getEndTime()
            );

            Appointment result = customerUpdateAppointmentUseCase.execute(action);

            assertThat(result.getStatus()).isEqualTo(BOOKED);
            assertThat(result.getSlotId()).isEqualTo(newSlot.getId());

            //VERIFY slot is released
            Slot slotAfter = slotService.getSlot(result.getSlotId()).orElseThrow();
            // verify new slot
            assertThat(slotAfter.getId()).isEqualTo(newSlot.getId());
            assertThat(slotAfter.getBookingCount()).isEqualTo(newSlot.getBookingCount()+1);
            // verify old slot
            Slot oldSlotAfterReschedule = slotService.getSlot(bookedAppointment.getSlotId()).orElseThrow();
            assertThat(oldSlot.getId()).isEqualTo(oldSlotAfterReschedule.getId())
                    .isNotEqualTo(slotAfter.getId()).isNotEqualTo(newSlot.getId());

            assertThat(oldSlot.getBookingCount()-1).isEqualTo(oldSlotAfterReschedule.getBookingCount());
            assertThat(oldSlotAfterReschedule.getStatus()).isEqualTo(SlotStatus.AVAILABLE);



        }

        @Test
        @DisplayName("Should publish CustomerRescheduledAppointmentEvent on reschedule")
        void shouldPublishRescheduledEvent() throws JsonProcessingException {
            Slot newSlot = slots.get(1);
            LocalDateTime newDateTime = LocalDateTime.of(newSlot.getDay(), newSlot.getStartTime());

            var action = new CustomerUpdateAppointmentAction.Reschedule(
                    bookedAppointment.getId(),
                    newSlot.getId(),
                    newDateTime,
                    newSlot.getEndTime()
            );

            customerUpdateAppointmentUseCase.execute(action);


            // Poll for Kafka event (CORRECT TOPIC)
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(
                    testConsumer,
                    Duration.ofSeconds(10)
            );
            ConsumerRecord<String, String> received = StreamSupport.stream(records.spliterator(), false)
                    .reduce((first, second) ->
                            second.key().equals(bookedAppointment.getId().toString()) &&second.timestamp()>first.timestamp()?second:first)
                    .orElseThrow(() -> new AssertionError("No records found"));

            // Assertions on Kafka event
            AssertionsForClassTypes.assertThat(received).isNotNull();
            AssertionsForClassTypes.assertThat(received.key()).isNotNull();
            String value = received.value();
            AssertionsForClassTypes.assertThat(value).isNotNull();

            ObjectMapper mapper = EventToJSONMapper.getMapper();

            ExtendedEventValue<AppointmentMetadata> extendedEventValue = mapper.readValue(value, AppointmentEventValueImpl.class);
            AssertionsForClassTypes.assertThat(extendedEventValue).isNotNull();
            AppointmentMetadata metadata = extendedEventValue.getMetadata();
            AssertionsForClassTypes.assertThat(metadata).isNotNull();
            assertThat(metadata.id()).isEqualTo(bookedAppointment.getId());
            AssertionsForClassTypes.assertThat(metadata.branchId()).isEqualTo(bookedAppointment.getBranchId());
            AssertionsForClassTypes.assertThat(metadata.customerUsername()).isEqualTo(bookedAppointment.getCustomerUsername());
            assertThat(metadata.createdAt().toLocalDate()).isEqualTo(LocalDate.now());
            Map<String, Object> stringObjectMap = metadata.otherData();
            var triggeredBy =  mapper.convertValue(stringObjectMap.get("triggerBy"), String.class);
            assertThat(triggeredBy).isNotNull().isEqualTo(EventTrigger.CUSTOMER.name());
            var fromState =  mapper.convertValue(stringObjectMap.get("fromState"), String.class);
            assertThat(fromState).isNotNull().isEqualTo(BOOKED.name());
            var toState =  mapper.convertValue(stringObjectMap.get("toState"), String.class);
            assertThat(toState).isNotNull().isEqualTo(BOOKED.name());
            AssertionsForClassTypes.assertThat(bookedAppointment.getReference()).isEqualTo(metadata.reference());
            AssertionsForClassTypes.assertThat(extendedEventValue.getSource()).isNotNull().isEqualTo("Appointment context");

        }

        @Test
        @DisplayName("Should throw NOT_FOUND when rescheduling non-existent appointment")
        void shouldThrowNotFoundWhenReschedulingNonExistent() {
            Slot newSlot = slots.get(1);

            var action = new CustomerUpdateAppointmentAction.Reschedule(
                    UUID.randomUUID(),
                    newSlot.getId(),
                    LocalDateTime.now().plusDays(1),
                    newSlot.getEndTime()
            );

            assertThatThrownBy(() -> customerUpdateAppointmentUseCase.execute(action))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

}
