package capitec.branch.appointment.event.infrastructure.kafka.producer;

import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.event.app.Topics;
import capitec.branch.appointment.event.app.port.AppointmentEventPort;
import capitec.branch.appointment.event.app.port.OTPEventProducerServicePort;
import capitec.branch.appointment.event.app.port.UserEventListenerPort;
import capitec.branch.appointment.kafka.app.EventPublishUseCase;
import capitec.branch.appointment.utils.sharekernel.metadata.AppointmentMetadata;
import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.KafkaProperties;
import capitec.branch.appointment.utils.sharekernel.metadata.MetaData;
import capitec.branch.appointment.utils.sharekernel.metadata.OTPMetadata;
import capitec.branch.appointment.utils.OTPCode;
import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
@Validated
public class EventPublisher implements OTPEventProducerServicePort, UserEventListenerPort, AppointmentEventPort {

    private final KafkaProperties kafkaProperties;
    private final EventPublishUseCase<String, MetaData> eventPublishUseCase;

    @Override
    public CompletableFuture<Boolean> sendRegistrationEvent(@Username String username,
                                                            @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS) String email,
                                                            @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME) String fullname,
                                                            @OTPCode String otpCode,
                                                            @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId) {
        String topic = Topics.REGISTRATION_EVENT;
        validateTopic(topic, traceId);
        var metadata = new OTPMetadata(fullname, username, email, otpCode);
        String key = username + otpCode;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,metadata, traceId, topic, key, LocalDateTime.now());
        log.debug("Sending registration event to kafka {}", eventValue);
        return publishAsync(eventValue);
    }

    @Override
    public void handleUserVerifiedEvent(String username, String email, String fullName,String otp, String traceId) {
        String topic = Topics.EMAIL_VERIFIED_EVENT;
        validateTopic(topic, traceId);
        var metadata = new OTPMetadata(fullName, username, email,otp);
        LocalDateTime now = LocalDateTime.now();
        var key = username + now;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,metadata, traceId, topic, key, now);
        log.debug("Sending verified event to kafka {}", eventValue);
        sendMessage().accept(publishAsync(eventValue), eventValue);
    }

    @Override
    public void handleDeleteUserEvent(String username, String email, String fullname, String otp, String traceId) {
        String topic = Topics.DELETE_USER_ACCOUNT_EVENT;
        validateTopic(topic, traceId);
        var metadata = new OTPMetadata(fullname, username, email, otp);
        var key = username + otp;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,metadata, traceId, topic, key, LocalDateTime.now());
        log.debug("Sending delete user event to kafka {}", eventValue);
        sendMessage().accept(publishAsync(eventValue), eventValue);
    }

    @Override
    public void handlePasswordUpdatedEvent(String username, String email, String fullname, String otp, String traceId) {
        String topic = Topics.PASSWORD_UPDATED_EVENT;
        validateTopic(topic, traceId);

        var metadata = new OTPMetadata(fullname, username, email, otp);
        var key = username + otp;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,metadata, traceId, topic, key, LocalDateTime.now());
        log.debug("Sending password updated event to kafka {}", eventValue);
        sendMessage().accept(publishAsync(eventValue), eventValue);
    }

    @Override
    public CompletableFuture<Boolean> sendPasswordResetRequestEvent(@Username String username,
                                                                    @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS) String email,
                                                                    @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME) String fullname,
                                                                    @OTPCode String OTP,
                                                                    @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId) {
        String topic = Topics.PASSWORD_RESET_REQUEST_EVENT;
        validateTopic(topic, traceId);
        var metadata = new OTPMetadata(fullname, username, email, OTP);
        var key = username + OTP;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,metadata, traceId, topic, key, LocalDateTime.now());
        log.debug("Sending password reset request event to kafka {}", eventValue);
        return publishAsync(eventValue);
    }

    @Override
    public CompletableFuture<Boolean> deleteUserRequestEvent(@Username String username,
                                                             @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS) String email,
                                                             @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME) String fullname,
                                                             @OTPCode String OTP,
                                                             @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId) {
        String topic = Topics.DELETE_USER_ACCOUNT_REQUEST_EVENT;
        validateTopic(topic, traceId);
        var metadata = new OTPMetadata(fullname, username, email, OTP);
        var key = username + OTP;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,metadata, traceId, topic, key, LocalDateTime.now());

        log.debug("Sending user request event to kafka {}", eventValue);
        return publishAsync(eventValue);
    }


    @Override
    public void publishEventAppointmentBooked(UUID id, String reference, String branchId, String customerUsername, LocalDate day,
                                              LocalTime startTime, LocalTime endTime,LocalDateTime occurredAt) {
        var traceId = UUID.randomUUID().toString();

        String topic = Topics.APPOINTMENT_BOOKED;
        validateTopic(topic, traceId);

        Map<String, Object> map = Map.of("day", day, "startTime", startTime, "endTime", endTime);
        var appointmentMetadata = new AppointmentMetadata(id, reference, branchId, customerUsername,occurredAt , map);
        String key = id + reference;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,appointmentMetadata, traceId, topic, key, LocalDateTime.now());
        log.debug("Sending booked appointment event to kafka {}", eventValue);
        sendMessage().accept(publishAsync(eventValue), eventValue);

    }

    @Override
    public void publishEventAttendAppointment(@NotNull UUID appointmentId, @NotBlank String appointmentReference,
                                              @Username String customerUsername, String branchId, AppointmentStatus fromState,
                                              @NotNull AppointmentStatus toState, String triggeredBy, @NotNull
                                              LocalDateTime occurredAt) {
        var traceId = UUID.randomUUID().toString();

        String topic = Topics.ATTENDED_APPOINTMENT;
        validateTopic(topic, traceId);
        Map<String, Object> map = Map.of(
                "fromState", fromState,
                "toState", toState, "triggerBy",
                triggeredBy, "occurredAt", occurredAt
        );
        var appointmentMetadata = new AppointmentMetadata(
                appointmentId, appointmentReference,
                branchId, customerUsername, occurredAt, map
        );

        String key = appointmentId + appointmentReference;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,appointmentMetadata, traceId, topic, key, LocalDateTime.now());
        log.debug("Sending attend appointment event to kafka {}", eventValue);
        sendMessage().accept(publishAsync(eventValue), eventValue);
    }

    @Override
    public void publishEventCustomerCancelAppointment(UUID appointmentId, String reference, String customerUsername, String branchId,
                                                      AppointmentStatus previousState, AppointmentStatus appointmentStatus,
                                                      String triggeredBy,
                                                      @NotNull LocalDateTime occurredAt) {
        var traceId = UUID.randomUUID().toString();
        log.debug("To published  appointment cancel event, branchId:{}, reference:{},appointmentId:{},occurred at:{}, trigger by:{} ",branchId,reference,appointmentId,
                occurredAt,triggeredBy);

        String topic = Topics.APPOINTMENT_CANCELED;
        validateTopic(topic, traceId);
        Map<String, Object> map = Map.of(
                "fromState", previousState.name(),
                "toState", appointmentStatus.name(),
                "triggerBy", triggeredBy
        );
        var appointmentMetadata = new AppointmentMetadata(
                appointmentId, reference, branchId,
                customerUsername, occurredAt, map
        );
        String key = appointmentId + reference;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,appointmentMetadata, traceId, topic, key, LocalDateTime.now());
        log.debug("Sending cancel event to kafka {}", eventValue);
        sendMessage().accept(publishAsync(eventValue), eventValue);

    }

    @Override
    public void publishEventCustomerRescheduleAppointment(UUID appointmentId, String reference, String customerUsername,
                                                          AppointmentStatus previousState, AppointmentStatus appointmentStatus,
                                                          String branchId, String triggeredBy, @NotNull LocalDateTime occurredAt) {
        var traceId = UUID.randomUUID().toString();

        String topic = Topics.APPOINTMENT_RESCHEDULED;
        validateTopic(topic, traceId);
        Map<String, Object> map = Map.of(
                "fromState", previousState.name(),
                "toState", appointmentStatus.name(),
                "triggerBy", triggeredBy
        );
        var appointmentMetadata = new AppointmentMetadata(
                appointmentId, reference, branchId, customerUsername, occurredAt, map
        );
        String key = appointmentId + reference;
        EventValue<String,MetaData> eventValue = new EventValue.OriginEventValue<>(key,appointmentMetadata, traceId, topic, key, LocalDateTime.now());
        log.debug("Sending  reschedule event to kafka {}", eventValue);
        sendMessage().accept(publishAsync(eventValue), eventValue);
    }


    public  CompletableFuture<Boolean> publishAsync(EventValue<String,MetaData> eventValue) {
        return eventPublishUseCase.publishEventAsync(eventValue);
    }

    private void validateTopic(String topic, String traceId) {
        if (!kafkaProperties.getTopicNames().contains(topic)) {
            log.error("Kafka topic {} is not configured. traceId: {}", topic, traceId);
            throw new InternalServerErrorException("Internal server error.");
        }
    }

    private BiConsumer<CompletableFuture<Boolean>, EventValue<String,MetaData>> sendMessage() {
        return (future, value) -> future.whenComplete((result, error) -> {
            if (error != null || !Boolean.TRUE.equals(result)) {
                log.error("Failed to publish {}, traceId: {}", value.topic(), value.traceId());
            } else {
                log.info("Published  successfully event:{}, traceId: {}", value.topic(), value.traceId());
            }
        });
    }


}
