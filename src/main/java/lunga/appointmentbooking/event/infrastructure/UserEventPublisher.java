package lunga.appointmentbooking.event.infrastructure;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunga.appointmentbooking.*;
import lunga.appointmentbooking.event.app.RetryEventPublisherSchedulerService;
import lunga.appointmentbooking.event.app.Topics;
import lunga.appointmentbooking.otp.domain.OTPEventProducerService;
import lunga.appointmentbooking.user.app.DeleteUserEvent;
import lunga.appointmentbooking.user.app.PasswordUpdatedEvent;
import lunga.appointmentbooking.user.app.UserVerifiedEvent;
import lunga.appointmentbooking.utils.OTPCode;
import lunga.appointmentbooking.utils.Username;
import lunga.appointmentbooking.utils.Validator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import lunga.appointmentbooking.kafka.app.EventPublishUseCase;
import lunga.appointmentbooking.kafka.infrastructure.configuration.properties.KafkaProperties;
import lunga.appointmentbooking.kafka.infrastructure.configuration.properties.ProducerProperties;
import lunga.appointmentbooking.kafka.domain.*;
import lunga.appointmentbooking.kafka.user.UserDefaultErrorEvent;
import lunga.appointmentbooking.kafka.user.UserDefaultEvent;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static lunga.appointmentbooking.kafka.infrastructure.event.KafkaEventPublisher.isInstanceOfRetryableExceptions;

@Slf4j
@RequiredArgsConstructor
@Component
@Validated
public class UserEventPublisher implements OTPEventProducerService, RetryEventPublisherSchedulerService {

    private final EventPublisher<String,EventValue> eventPublisher;
    private final ProducerProperties producerProperties;
    private final KafkaProperties kafkaProperties;
    private final EventPublishUseCase eventPublishUseCase;


    private Predicate<String> isValidTopicName(){
        return topicName -> StringUtils.isNotBlank(topicName) &&
                kafkaProperties.getTopicNames().stream().anyMatch(topicName::equals);
    }

    @Override
    public CompletableFuture<Boolean>  sendRegistrationEvent(@Username String username,
                                                             @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS) String email,
                                                             @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME)
                                         String fullname,
                                                             @OTPCode String otpCode, @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId) {



        if ( !isValidTopicName().test(Topics.REGISTRATION_EVENT)) {
            log.error("Error: Kafka topic for registration is not configured. traceId: {}", traceId);
            throw new InternalServerErrorException("Internal sever error.");
        }


        var value = new UserDefaultEvent(Topics.REGISTRATION_EVENT, otpCode,traceId, UUID.randomUUID().toString(), LocalDateTime.now(),
                fullname,String.valueOf(username),String.valueOf(username),email);

       return publishAsync(value);
    }

    @EventListener(UserVerifiedEvent.class)
    public void sendUserVerifiedEvent(@Valid UserVerifiedEvent userVerifiedEvent) {


        String traceId = userVerifiedEvent.traceId();

        if (!isValidTopicName().test(Topics.EMAIL_VERIFIED_EVENT)) {
            log.error("Error: Kafka topic for email verified is not configured. traceId: {}", traceId);
            throw new InternalServerErrorException("Internal sever error.");
        }

        String username = String.valueOf(userVerifiedEvent.username());
        var value = new UserDefaultEvent(Topics.EMAIL_VERIFIED_EVENT, userVerifiedEvent.fullName(),traceId, UUID.randomUUID().toString(), LocalDateTime.now(),
                userVerifiedEvent.fullName(), username, username, userVerifiedEvent.email());
        publishAsync(value)
                .whenComplete((r, err)->{

                    if(err !=null || r ==null || !r){

                        log.error("Failed to  publish UserVerifiedEvent, traceId:{}",value.getTraceId());
                    }
                    else {

                        log.info("Published UserVerifiedEvent successfully, traceId:{}", traceId);
                    }
                });
    }

//    @EventListener(WelcomeEvent.class)
//    public void sendWelcomeEvent(@Valid WelcomeEvent welcomeEvent) {
//
//        var fullName = welcomeEvent.fullname();
//        var traceId = welcomeEvent.traceId();
//
//        if (!isValidTopicName().test(Topics.COMPLETE_REGISTRATION_EVENT )) {
//            log.error("Error: Kafka topic for welcome event  is not configured. traceId: {}", traceId);
//            throw new InternalServerErrorException("Internal sever error.");
//        }
//        var value = new UserDefaultEvent(Topics.COMPLETE_REGISTRATION_EVENT, fullName,traceId, UUID.randomUUID().toString(), LocalDateTime.now(),
//               String.valueOf(welcomeEvent.username()),welcomeEvent.email());
//
//
//        sendMessage().accept(publishAsync(value),value);
//    }

    @EventListener(DeleteUserEvent.class)
    public void sendGoodByMessage(@Valid DeleteUserEvent event) {

        var traceId = event.traceId();

        if (!isValidTopicName().test(Topics.DELETE_USER_ACCOUNT_EVENT)) {

            log.error("Error: Kafka topic for delete user account event  is not configured. traceId: {}", traceId);
            throw new InternalServerErrorException("Internal sever error.");
        }
        String username = String.valueOf(event.username());
        var value = new UserDefaultEvent(Topics.DELETE_USER_ACCOUNT_EVENT, event.fullname(),traceId, UUID.randomUUID().toString(), LocalDateTime.now(), username, event.email());


         sendMessage().accept(publishAsync(value),value);
    }

    @Override
    public CompletableFuture<Boolean>  sendPasswordResetRequestEvent(
            @Username String username,
            @NotBlank(message = Validator.EMAIL_MESS)
            @Email(message = Validator.EMAIL_MESS) String email,
            @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME)
            String fullname,
            @OTPCode String OTP,
            @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId) {


        if (!isValidTopicName().test(Topics.PASSWORD_RESET_REQUEST_EVENT)) {

            log.error("Error: Kafka topic for password reset request is not configured. traceId: {}", traceId);
            throw new InternalServerErrorException("Internal sever error.");
        }
        String key = String.valueOf(username);
        var value = new UserDefaultEvent(Topics.PASSWORD_RESET_REQUEST_EVENT, OTP,traceId, UUID.randomUUID().toString(), LocalDateTime.now(),
                fullname, key, email);

       return publishAsync(value);
    }

    @EventListener(PasswordUpdatedEvent.class)
    public void sendPasswordUpdatedEvent(@Valid PasswordUpdatedEvent passwordUpdatedEvent) {


        if (!isValidTopicName().test(Topics.PASSWORD_UPDATED_EVENT)) {

            log.error("Error: Kafka topic for password updated event  is not configured. traceId: {}", passwordUpdatedEvent.traceId());
            throw new InternalServerErrorException("Internal sever error.");
        }
        String key = String.valueOf(passwordUpdatedEvent.username());
        var value = new UserDefaultEvent(Topics.PASSWORD_UPDATED_EVENT, passwordUpdatedEvent.otp(), passwordUpdatedEvent.traceId(), UUID.randomUUID().toString(), LocalDateTime.now(),
                 passwordUpdatedEvent.fullname(),key,passwordUpdatedEvent.email());

        sendMessage().accept(publishAsync(value),value);
    }

    @Override
    public CompletableFuture<Boolean>  deleteUserRequestEvent(@Username String username, @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS) String email,
                                          @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME) String fullname, @OTPCode String OTP,
                                          @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId) {



        if (!isValidTopicName().test(Topics.DELETE_USER_ACCOUNT_REQUEST_EVENT)) {

            log.error("Error: Kafka topic for delete user/account request is not configured. traceId: {}", traceId);

            throw new InternalServerErrorException("Internal sever error.");
        }

        String key = String.valueOf(username);
        var value = new UserDefaultEvent(Topics.DELETE_USER_ACCOUNT_REQUEST_EVENT, OTP, traceId, UUID.randomUUID().toString(), LocalDateTime.now(),
                fullname,key, key, email );

        return publishAsync(value);
    }


    @EventListener(DeleteUserEvent.class)
    public void deleteUserEvent(DeleteUserEvent deleteUserEvent) {


        String traceId = deleteUserEvent.traceId();

        if (!isValidTopicName().test(Topics.DELETE_USER_ACCOUNT_EVENT)) {

            log.error("Error: Kafka topic for delete user/account  is not configured. traceId: {}", traceId);

            throw new InternalServerErrorException("Internal sever error.");
        }


        String username = String.valueOf(deleteUserEvent.username());

        var value = new UserDefaultEvent(Topics.DELETE_USER_ACCOUNT_EVENT, deleteUserEvent.OTP(), traceId, UUID.randomUUID().toString(), LocalDateTime.now(),
                deleteUserEvent.fullname(), username, username, deleteUserEvent.email());

         sendMessage().accept(publishAsync(value),value);

    }

    private BiConsumer<CompletableFuture<Boolean>,EventValue > sendMessage() {


       return   (future,value)-> future.whenComplete((r, err)->{

            if(err !=null || r ==null || !r){

                log.error("Failed publish {} , traceId:{}",value.getTopic(),value.getTraceId());
            }
            else {

                log.info("Published  {} successfully, traceId:{}",value.getTopic(), value.getTraceId());
            }
        });

    }

    @Override
    public  CompletableFuture<Boolean> republishEvent(ErrorEventValue  userEvent) {

        if (userEvent == null) {
            log.error("Invalid Producer Record for the retry");
            throw new InternalServerErrorException("Internal sever error.");
        }

        return publishAsync(userEvent);

    }

    public CompletableFuture<Boolean>  publishAsync(EventValue userEventValue) {

      return   eventPublisher.publishAsync(userEventValue.getKey(),userEventValue)
                .handle(((results, throwable) ->this.callback(results)))
              .thenApplyAsync(eventPublishUseCase.callback());
    }





    public <I extends Serializable, T extends EventValue> PublisherResults<I, T> callback( PublisherResults<String, EventValue> results) {

        EventValue event = results.event();

        Throwable throwable = results.exception();

        PublisherResults<I, T> finalResults;

        if(throwable==null){

            if(event instanceof  ErrorEventValue errorEventValue) {
                errorEventValue.setRetryCount(errorEventValue.getRetryCount() + 1);
                errorEventValue.setDeadLetterStatus(DEAD_LETTER_STATUS.RECOVERED);
                errorEventValue.setPartition(results.partition());
                errorEventValue.setOffset(results.offset());
                finalResults = new PublisherResults<>((T)errorEventValue, (I)errorEventValue.getKey(), errorEventValue.getPartition(),
                        errorEventValue.getOffset(), null);
            }
            else{

                finalResults = new PublisherResults<>((T)event, (I)event.getKey(), results.partition(), results.offset(), null);

            }
        }
        else {

            if( event instanceof  ErrorEventValue errorEventValue ) {

                errorEventValue.setRetryCount(errorEventValue.getRetryCount() + 1);
                finalResults = new PublisherResults<>((T)errorEventValue, (I)errorEventValue.getKey(), errorEventValue.getPartition(),
                        errorEventValue.getOffset(), throwable);
            }
            else {

                DEAD_LETTER_STATUS status = DEAD_LETTER_STATUS.DEAD;

                try {
                    if (isInstanceOfRetryableExceptions().apply(throwable, producerProperties.getRetryableExceptions())) {

                        var defaultErrorEventValue = errorValue(results, event,throwable,true,0,status);
                        finalResults = new PublisherResults<>((T)defaultErrorEventValue, (I)defaultErrorEventValue.getKey(), defaultErrorEventValue.getPartition(),
                                defaultErrorEventValue.getOffset(), throwable);


                    } else {

                        DefaultErrorEventValue defaultErrorEventValue = errorValue(results, event, throwable, false, 0, status);
                        finalResults = new PublisherResults<>((T)defaultErrorEventValue, (I)defaultErrorEventValue.getKey(), defaultErrorEventValue.getPartition(),
                                defaultErrorEventValue.getOffset(), throwable);
                    }

                } catch (Exception e) {

                    log.error("Failed to save dead letter to database, traceId:{}",event.getTraceId(), e);

                    DefaultErrorEventValue defaultErrorEventValue = errorValue(results, event, throwable, false, 0, status);
                    finalResults = new PublisherResults<>((T)defaultErrorEventValue, (I)defaultErrorEventValue.getKey(), defaultErrorEventValue.getPartition(),
                            defaultErrorEventValue.getOffset(), e);
                }
            }
        }
        return finalResults;
    }

    private UserDefaultErrorEvent errorValue(PublisherResults< String , EventValue> results, EventValue event, Throwable throwable, boolean isRetryable, int retryCount, DEAD_LETTER_STATUS status){

        Throwable cause = throwable.getCause();

        String exception = cause != null ? cause.getMessage() : throwable.getMessage();
        var causeClass= cause != null ? cause.getClass().getName() :exception.getClass().toString();
        String stackTrace = throwable.getStackTrace()!=null&&throwable.getStackTrace().length!=0 ? Arrays.toString(throwable.getStackTrace()) :
                throwable.fillInStackTrace().toString();


        String email=null,username=null,fullname=null;

        if(event instanceof UserDefaultEvent || event instanceof UserDefaultErrorEvent){

            fullname =event instanceof UserDefaultEvent eventValueModel? eventValueModel.getFullname():((UserDefaultErrorEvent)event).getFullname();
            username=   event instanceof UserDefaultEvent eventValueModel? eventValueModel.getUsername():((UserDefaultErrorEvent)event).getUsername();
            email=event instanceof UserDefaultEvent eventValueModel? eventValueModel.getEmail():((UserDefaultErrorEvent)event).getEmail();
        }

        return    new UserDefaultErrorEvent(event.getTopic(), event.getValue(), event.getTraceId(), event.getEventId(),
                event.getPublishTime(), results.partition(), results.offset(), results.key(),
                exception,throwable.getClass().getName(),causeClass,stackTrace,isRetryable,retryCount, status,fullname,username,email
        );
    }
}
