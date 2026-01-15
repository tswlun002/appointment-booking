package capitec.branch.appointment.event.infrastructure.kafka;

import capitec.branch.appointment.event.app.RetryEventPublisherSchedulerService;
import capitec.branch.appointment.event.app.Topics;
import capitec.branch.appointment.event.app.port.OTPEventProducerServicePort;
import capitec.branch.appointment.event.app.port.UserEventListenerPort;
import capitec.branch.appointment.kafka.app.EventPublishUseCase;
import capitec.branch.appointment.kafka.domain.ErrorEventValue;
import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.KafkaProperties;
import capitec.branch.appointment.kafka.user.UserMetadata;
import capitec.branch.appointment.utils.OTPCode;
import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
@Validated
public class UserEventPublisher implements OTPEventProducerServicePort, RetryEventPublisherSchedulerService, UserEventListenerPort {

    private final KafkaProperties kafkaProperties;
    private final EventPublishUseCase eventPublishUseCase;

    @Override
    public CompletableFuture<Boolean> sendRegistrationEvent(@Username String username,
                                                            @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS) String email,
                                                            @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME) String fullname,
                                                            @OTPCode String otpCode,
                                                            @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId) {
        validateTopic(Topics.REGISTRATION_EVENT, traceId);
        var event = createUserKafEventPort(Topics.REGISTRATION_EVENT, otpCode, traceId,
                new UserMetadata(fullname, username, email));
        return publishAsync(event);
    }

    @Override
    public void handleUserVerifiedEvent(String username, String email, String fullName, String traceId) {
        validateTopic(Topics.EMAIL_VERIFIED_EVENT, traceId);
        var kafkaEvent = createUserKafEventPort(Topics.EMAIL_VERIFIED_EVENT,fullName,
                traceId, new UserMetadata(fullName, username, email));
        sendMessage().accept(publishAsync(kafkaEvent), kafkaEvent);
    }

    @Override
    public void handleDeleteUserEvent(String username, String email, String fullname, String otp, String traceId) {
        validateTopic(Topics.DELETE_USER_ACCOUNT_EVENT, traceId);
        var kafkaEvent = createUserKafEventPort(Topics.DELETE_USER_ACCOUNT_EVENT, otp,
                traceId, new UserMetadata(fullname, username,email));
        sendMessage().accept(publishAsync(kafkaEvent), kafkaEvent);
    }

    @Override
    public void handlePasswordUpdatedEvent(String username, String email, String fullname, String otp, String traceId) {
        validateTopic(Topics.PASSWORD_UPDATED_EVENT, traceId);
        var kafkaEvent = createUserKafEventPort(Topics.PASSWORD_UPDATED_EVENT, otp,
                traceId, new UserMetadata(fullname, username,email));
        sendMessage().accept(publishAsync(kafkaEvent), kafkaEvent);
    }

    @Override
    public CompletableFuture<Boolean> sendPasswordResetRequestEvent(@Username String username,
                                                                    @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS) String email,
                                                                    @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME) String fullname,
                                                                    @OTPCode String OTP,
                                                                    @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId) {
        validateTopic(Topics.PASSWORD_RESET_REQUEST_EVENT, traceId);
        var event = createUserKafEventPort(Topics.PASSWORD_RESET_REQUEST_EVENT, OTP, traceId,
                new UserMetadata(fullname, username, email));
        return publishAsync(event);
    }

    @Override
    public CompletableFuture<Boolean> deleteUserRequestEvent(@Username String username,
                                                             @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS) String email,
                                                             @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME) String fullname,
                                                             @OTPCode String OTP,
                                                             @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId) {
        validateTopic(Topics.DELETE_USER_ACCOUNT_REQUEST_EVENT, traceId);
        var event = createUserKafEventPort(Topics.DELETE_USER_ACCOUNT_REQUEST_EVENT, OTP, traceId,
                new UserMetadata(fullname, username, email));
        return publishAsync(event);
    }

    @Override
    public CompletableFuture<Boolean> republishEvent(ErrorEventValue userEvent) {
        Asserts.notNull(userEvent, "userEvent");
        return publishAsync(userEvent);
    }

    public CompletableFuture<Boolean> publishAsync(EventValue userEventValue) {
        return eventPublishUseCase.publishEventAsync(userEventValue);
    }

    private void validateTopic(String topic, String traceId) {
        if (!kafkaProperties.getTopicNames().contains(topic)) {
            log.error("Kafka topic {} is not configured. traceId: {}", topic, traceId);
            throw new InternalServerErrorException("Internal server error.");
        }
    }

    private UserEventValueImpl createUserKafEventPort(String topic, String key, String traceId, UserMetadata metadata) {
        return new UserEventValueImpl(
                UUID.randomUUID().toString(),
                topic,
                key,
                traceId,
                LocalDateTime.now(),
                metadata
        );
    }

    private BiConsumer<CompletableFuture<Boolean>, EventValue> sendMessage() {
        return (future, value) -> future.whenComplete((result, error) -> {
            if (error != null || !Boolean.TRUE.equals(result)) {
                log.error("Failed to publish {}, traceId: {}", value.getTopic(), value.getTraceId());
            } else {
                log.info("Published {} successfully, traceId: {}", value.getTopic(), value.getTraceId());
            }
        });
    }
}
