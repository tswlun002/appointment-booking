package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.event.app.Topics;
import capitec.branch.appointment.event.infrastructure.kafka.producer.appointment.AppointmentEventValueImpl;
import capitec.branch.appointment.kafka.appointment.AppointmentMetadata;
import capitec.branch.appointment.kafka.domain.ExtendedEventValue;
import capitec.branch.appointment.slots.app.GetSlotQuery;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import capitec.branch.appointment.user.app.GetUserQuery;
import capitec.branch.appointment.user.app.UsernameCommand;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.utils.sharekernel.EventToJSONMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;


@DisplayName("BookAppointmentUseCase Integration Test (Real Dependencies)")
class BookAppointmentUseCaseTest extends AppointmentTestBase {

    @Autowired
    private BookAppointmentUseCase bookAppointmentUseCase;
    @Autowired
    private GetUserQuery getUserQuery;
    private AppointmentDTO validAppointmentDTO;
    @Autowired
    private GetSlotQuery getSlotQuery;
    private Consumer<String, String> testConsumer;



    @BeforeEach
    void setup() {
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
        testConsumer.subscribe(List.of(Topics.APPOINTMENT_BOOKED));
    }
    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }



    @Test
    @DisplayName("Should execute successfully and publish event with real beans")
    void shouldExecuteSuccessfullyAndPublishEvent() throws JsonProcessingException {

        List<Slot> ListOneSlotADay = slots.stream().collect(Collectors.groupingBy(Slot::getDay)).values().stream().map(List::getFirst).toList();
        for (Slot slot : ListOneSlotADay) {


            Branch branch = branches.getFirst();
            String customerUsername = guestClients.getFirst();
            User user = getUserQuery.execute(new UsernameCommand(customerUsername));

            String serviceType = "Deposit";

            validAppointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), user.getUsername(), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());

            // 2. Execute the Use Case
            var result = bookAppointmentUseCase.execute(validAppointmentDTO) ;

            // 3. Assertions
            assertNotNull(result, "The execution should return true on successful event publication.");

            // Poll for Kafka event (CORRECT TOPIC)
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(
                    testConsumer,
                    Duration.ofSeconds(10)
            );
            ConsumerRecord<String, String> received = StreamSupport.stream(records.spliterator(), false)
                    .reduce((first, second) ->
                            second.key().equals(result.getId().toString()) &&  second.timestamp()>first.timestamp()?second:first)
                    .orElseThrow(() -> new AssertionError("No records found"));

            // Assertions on Kafka event
            assertThat(received).isNotNull();
            assertThat(received.key()).isNotNull();
            String value = received.value();
            assertThat(value).isNotNull();

            ObjectMapper mapper = EventToJSONMapper.getMapper();

            ExtendedEventValue<AppointmentMetadata> extendedEventValue = mapper.readValue(value, AppointmentEventValueImpl.class);
            assertThat(extendedEventValue).isNotNull();
            AppointmentMetadata metadata = extendedEventValue.getMetadata();
            assertThat(metadata).isNotNull();
            assertThat(metadata.branchId()).isEqualTo(branch.getBranchId());
            assertThat(metadata.customerUsername()).isEqualTo(user.getUsername());
            Map<String, Object> stringObjectMap = metadata.otherData();

            var startTime =  mapper.convertValue( stringObjectMap.get("startTime"),LocalTime.class);
            assertThat(startTime).isNotNull().isEqualTo(slot.getStartTime());
            var endTime =  mapper.convertValue( stringObjectMap.get("endTime"),LocalTime.class);
            assertThat(endTime).isNotNull().isEqualTo(slot.getEndTime());
            var day =  mapper.convertValue(stringObjectMap.get("day"), LocalDate.class);
            assertThat(day).isNotNull().isEqualTo(slot.getDay());
            Optional<Appointment> byId = appointmentService.findById(metadata.id());
            assertThat(byId).isPresent();
            assertThat(byId.get().getReference()).isEqualTo(metadata.reference());
            assertThat(extendedEventValue.getSource()).isNotNull().isEqualTo("Appointment context");


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
        var result = bookAppointmentUseCase.execute(validAppointmentDTO);
        assertThat(result).isNotNull();
        // 2. Poll for records (wait up to 10 seconds)
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(
                testConsumer,
                Duration.ofSeconds(10)
        );
        Appointment finalResult = result;
        ConsumerRecord<String, String> received = StreamSupport.stream(records.spliterator(), false)
                .reduce((first, second) ->
                        second.key().equals(finalResult.getId().toString()) &&second.timestamp()>first.timestamp()?second:first)
                .orElseThrow(() -> new AssertionError("No records found"));
        // 3. Assertions
        assertThat(received).isNotNull();
        assertThat(received.key()).isNotNull();
        String value = received.value();
        assertThat(value).isNotNull();


        // Another user pick same slot
        AppointmentDTO appointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), guestClients.get(1), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());
        result = bookAppointmentUseCase.execute(appointmentDTO) ;
        user = getUserQuery.execute(new UsernameCommand(guestClients.get(1)));
        assertThat(result).isNotNull();
        Appointment finalResult1 = result;
        received = StreamSupport.stream(records.spliterator(), false)
                .reduce((first, second) ->
                        second.key().equals(finalResult1.getId().toString()) &&second.timestamp()>first.timestamp()?second:first)
                .orElseThrow(() -> new AssertionError("No records found"));
        // 3. Assertions
        assertThat(received).isNotNull();
        assertThat(received.key()).isNotNull();
         value = received.value();
        assertThat(value).isNotNull();
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
        var result = bookAppointmentUseCase.execute(validAppointmentDTO) ;
        assertThat(result).isNotNull();
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(
                testConsumer,
                Duration.ofSeconds(10)
        );
        Appointment finalResult1 = result;
        ConsumerRecord<String, String> received = StreamSupport.stream(records.spliterator(), false)
                .reduce((first, second) ->
                        second.key().equals(finalResult1.getId().toString()) && second.timestamp()>first.timestamp()?second:first)
                .orElseThrow(() -> new AssertionError("No records found"));
        // 3. Assertions
        assertThat(received).isNotNull();
        assertThat(received.key()).isNotNull();
        String value = received.value();
        assertThat(value).isNotNull();

        // 2. Execute the Use Case
        validAppointmentDTO = new AppointmentDTO(slot.getId(), branch.getBranchId(), guestClients.get(1), serviceType,slot.getDay(),slot.getStartTime(),slot.getEndTime());
        result = bookAppointmentUseCase.execute(validAppointmentDTO);
        assertThat(result).isNotNull();
        records = KafkaTestUtils.getRecords(
                testConsumer,
                Duration.ofSeconds(10)
        );
        Appointment finalResult = result;
        received = StreamSupport.stream(records.spliterator(), false)
                .reduce((first, second) ->
                        second.key().equals(finalResult.getId().toString()) &&second.timestamp()>first.timestamp()?second:first)
                .orElseThrow(() -> new AssertionError("No records found"));
        // 3. Assertions
        assertThat(received).isNotNull();
        assertThat(received.key()).isNotNull();
         value = received.value();
        assertThat(value).isNotNull();

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
        var bookedAppointment = bookAppointmentUseCase.execute(validAppointmentDTO) ;
        assertThat(bookedAppointment).isNotNull();
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(
                testConsumer,
                Duration.ofSeconds(10)
        );
        ConsumerRecord<String, String> received = StreamSupport.stream(records.spliterator(), false)
                .reduce((first, second) ->
                        second.key().equals(bookedAppointment.getId().toString()) &&second.timestamp()>first.timestamp()?second:first)
                .orElseThrow(() -> new AssertionError("No records found"));
        // 3. Assertions
        assertThat(received).isNotNull();
        assertThat(received.key()).isNotNull();
        String value = received.value();
        assertThat(value).isNotNull();

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