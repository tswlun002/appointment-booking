package capitec.branch.appointment.notification.infrastructure.kafka;

import capitec.branch.appointment.kafka.user.UserMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.kafka.user.UserDefaultErrorEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Component
public class EventConsumer {

    private final ApplicationEventPublisher eventPublisher;
    private  final EventMapperToEmail eventMapperToEmail;

    private Function<EventValue,String>  getOriginalTopic =(eventValue)-> eventValue.getTopic().substring(0, eventValue.getTopic().lastIndexOf("."));

    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.notification.domain.Notification).REGISTRATION_EVENT}",
            "#{T(capitec.branch.appointment.notification.domain.Notification).DELETE_USER_ACCOUNT_REQUEST_EVENT}",
            "#{T(capitec.branch.appointment.notification.domain.Notification).PASSWORD_RESET_REQUEST_EVENT}",
    },
            groupId = "otp-events",autoStartup = "${kafka.listen.auto.start:true}")
    public  void OTPEvents(ConsumerRecord<String, EventValue> consumerRecord)  {

        EventValue value = consumerRecord.value();

        log.info("Received otp type event: {}", value.getTraceId());

        if(value instanceof UserMetadata userMetadata){

            eventPublisher.publishEvent(eventMapperToEmail.userEventmapToOTPEmail(userMetadata));
        }
         else if(value instanceof UserDefaultErrorEvent userDefaultErrorEvent) {

             eventPublisher.publishEvent(eventMapperToEmail.userEventmapToOTPEmail(userDefaultErrorEvent));
        }

    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.notification.domain.Notification).OTPEmailPattern()}",
            groupId = "otp-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void recoveryOTPEvents(ConsumerRecord<String, EventValue> consumerRecord) {

        EventValue value = consumerRecord.value();
        log.info("Received recover otp type event: {}", value.getTraceId());

        if(value instanceof UserMetadata userMetadata){

                      userMetadata.setTopic(getOriginalTopic.apply(userMetadata));
            eventPublisher.publishEvent(eventMapperToEmail.userEventmapToOTPEmail(userMetadata));
        }
        else if(value instanceof UserDefaultErrorEvent userDefaultErrorEvent) {
            userDefaultErrorEvent.setTopic(getOriginalTopic.apply(userDefaultErrorEvent));

            eventPublisher.publishEvent(eventMapperToEmail.userEventmapToOTPEmail(userDefaultErrorEvent));
        }

    }



    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.notification.domain.Notification).COMPLETE_REGISTRATION_EVENT}",
            "#{T(capitec.branch.appointment.notification.domain.Notification).DELETE_USER_ACCOUNT_EVENT}",
            "#{T(capitec.branch.appointment.notification.domain.Notification).PASSWORD_UPDATED_EVENT}",
    },
            groupId = "confirmation-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void confirmationEvents(ConsumerRecord<String, EventValue> consumerRecord)  {

        EventValue value = consumerRecord.value();

        log.info("Received confirmation type event: {}", value.getTraceId());

        if(value instanceof UserMetadata userMetadata){

            eventPublisher.publishEvent(eventMapperToEmail.userEventMapToConfirmationEmail(userMetadata));
        }
        else if(value instanceof UserDefaultErrorEvent userDefaultErrorEvent) {

            eventPublisher.publishEvent(eventMapperToEmail.userEventMapToConfirmationEmail(userDefaultErrorEvent));
        }

    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.notification.domain.Notification).confirmationEmailPattern()}",
            groupId = "confirmation-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void recoveryConfirmationEvents(ConsumerRecord<String, EventValue> consumerRecord) {

        EventValue value = consumerRecord.value();
        log.info("Received recover confirmation type event: {}", value.getTraceId());

        if(value instanceof UserMetadata userMetadata){
            userMetadata.setTopic(getOriginalTopic.apply(userMetadata));

            eventPublisher.publishEvent(eventMapperToEmail.userEventMapToConfirmationEmail(userMetadata));
        }
        else if(value instanceof UserDefaultErrorEvent userDefaultErrorEvent) {
            userDefaultErrorEvent.setTopic(getOriginalTopic.apply(userDefaultErrorEvent));

            eventPublisher.publishEvent(eventMapperToEmail.userEventMapToConfirmationEmail(userDefaultErrorEvent));
        }

    }

}
