package capitec.branch.appointment.event.infrastructure.kafka.consumer;

import capitec.branch.appointment.kafka.appointment.AppointmentErrorEventValue;
import capitec.branch.appointment.kafka.appointment.AppointmentEventValue;
import capitec.branch.appointment.kafka.appointment.AppointmentMetadata;
import capitec.branch.appointment.kafka.domain.ExtendedEventValue;
import capitec.branch.appointment.kafka.user.UserErrorEventValue;
import capitec.branch.appointment.kafka.user.UserEventValue;
import capitec.branch.appointment.kafka.user.UserMetadata;
import capitec.branch.appointment.notification.domain.AppointmentBookedEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import capitec.branch.appointment.kafka.domain.EventValue;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Component
public class EventConsumerForEmail {

    private final ApplicationEventPublisher eventPublisher;
    private  final EventMapperToEmail eventMapperToEmail;

    private Function<EventValue,String>  getOriginalTopic =(eventValue)-> eventValue.getTopic().substring(0, eventValue.getTopic().lastIndexOf("."));

    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).REGISTRATION_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).DELETE_USER_ACCOUNT_REQUEST_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).PASSWORD_RESET_REQUEST_EVENT}",
    },
            groupId = "otp-events",autoStartup = "${kafka.listen.auto.start:true}")
    public  void OTPEvents(ConsumerRecord<String, ExtendedEventValue<UserMetadata>> consumerRecord)  {

        var value = consumerRecord.value();

        log.info("Received otp type event: {}", value.getTraceId());

        if(value instanceof UserEventValue eventValue){

            eventPublisher.publishEvent(eventMapperToEmail.userEventmapToOTPEmail(eventValue));
        }
         else if(value instanceof UserErrorEventValue userErrorEventValue) {

             eventPublisher.publishEvent(eventMapperToEmail.userEventmapToOTPEmail(userErrorEventValue));
        }

    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.event.app.Topics).OTPEmailPattern()}",
            groupId = "otp-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void recoveryOTPEvents(ConsumerRecord<String, ExtendedEventValue<UserMetadata>> consumerRecord) {

        var value = consumerRecord.value();
        log.info("Received recover otp type event: {}", value.getTraceId());

        if(value instanceof UserEventValue eventValue){

            String originalTopic = getOriginalTopic.apply(eventValue);

            eventPublisher.publishEvent(eventMapperToEmail.userEventmapToOTPEmail(eventValue, originalTopic));
        }
        else if(value instanceof UserErrorEventValue eventValue) {
            String originalTopic = getOriginalTopic.apply(eventValue);

            eventPublisher.publishEvent(eventMapperToEmail.userEventmapToOTPEmail(eventValue, originalTopic));
        }

    }



    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).COMPLETE_REGISTRATION_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).DELETE_USER_ACCOUNT_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).PASSWORD_UPDATED_EVENT}",
    },
            groupId = "confirmation-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void confirmationEvents(ConsumerRecord<String, ExtendedEventValue<UserMetadata>> consumerRecord)  {

        var value = consumerRecord.value();

        log.info("Received confirmation type event: {}", value.getTraceId());

        if(value instanceof UserEventValue eventValue){

            eventPublisher.publishEvent(eventMapperToEmail.userEventMapToConfirmationEmail(eventValue));
        }
        else if(value instanceof UserErrorEventValue userErrorEventValue) {

            eventPublisher.publishEvent(eventMapperToEmail.userEventMapToConfirmationEmail(userErrorEventValue));
        }

    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.event.app.Topics).confirmationEmailPattern()}",
            groupId = "confirmation-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void recoveryConfirmationEvents(ConsumerRecord<String, ExtendedEventValue<UserMetadata>> consumerRecord) {

        var value = consumerRecord.value();
        log.info("Received recover confirmation type event: {}", value.getTraceId());

        if(value instanceof UserEventValue eventValue){
            String originalTopic = getOriginalTopic.apply(eventValue);

            eventPublisher.publishEvent(eventMapperToEmail.userEventMapToConfirmationEmail(eventValue, originalTopic));
        }
        else if(value instanceof UserErrorEventValue errorEventValue) {
            String originalTopic = getOriginalTopic.apply(errorEventValue);

            eventPublisher.publishEvent(eventMapperToEmail.userEventMapToConfirmationEmail(errorEventValue,originalTopic));
        }

    }


    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).APPOINTMENT_RESCHEDULED}",
            "#{T(capitec.branch.appointment.event.app.Topics).APPOINTMENT_BOOKED}",
    },
            groupId = "booked-appointment-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void bookedAppointmentEvents(ConsumerRecord<String, ExtendedEventValue<AppointmentMetadata>> consumerRecord)  {

        var value = consumerRecord.value();

        log.info("Received confirmation type event: {}", value.getTraceId());

        if(value instanceof AppointmentEventValue eventValue){

            eventPublisher.publishEvent(eventMapperToEmail.appointmentBookedEmail(eventValue));
        }
        else if(value instanceof AppointmentErrorEventValue errorEventValue) {

            eventPublisher.publishEvent(eventMapperToEmail.appointmentBookedEmail(errorEventValue));
        }

    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.event.app.Topics).bookedAppointmentPattern()}",
            groupId = "booked-appointment-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void recoveryBookedAppointmentEvents(ConsumerRecord<String, ExtendedEventValue<AppointmentMetadata>> consumerRecord) {

        var value = consumerRecord.value();
        log.info("Received recover confirmation type event: {}", value.getTraceId());

        if(value instanceof AppointmentEventValue eventValue){
            String originalTopic = getOriginalTopic.apply(eventValue);

            AppointmentBookedEmail event = eventMapperToEmail.appointmentBookedEmail(eventValue, originalTopic);
            eventPublisher.publishEvent(event);
        }
        else if(value instanceof AppointmentErrorEventValue errorEventValue) {
            String originalTopic = getOriginalTopic.apply(errorEventValue);

            eventPublisher.publishEvent(eventMapperToEmail.appointmentBookedEmail(errorEventValue,originalTopic));
        }

    }

    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).APPOINTMENT_CANCELED}",
            "#{T(capitec.branch.appointment.event.app.Topics).ATTENDED_APPOINTMENT}"
    },
            groupId = "appointment-updates-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void appointmentUpdatesEvents(ConsumerRecord<String, ExtendedEventValue<AppointmentMetadata>> consumerRecord)  {

        var value = consumerRecord.value();

        log.info("Received confirmation type event: {}", value.getTraceId());

        if(value instanceof AppointmentEventValue eventValue){

            eventPublisher.publishEvent(eventMapperToEmail.appointmentStatusUpdatesEmail(eventValue));
        }
        else if(value instanceof AppointmentErrorEventValue errorEventValue) {

            eventPublisher.publishEvent(eventMapperToEmail.appointmentStatusUpdatesEmail(errorEventValue));
        }

    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.event.app.Topics).appointmentUpdatesPattern()}",
            groupId = "appointment-updates-events",autoStartup = "${kafka.listen.auto.start:true}")
    public void recoveryAppointmentUpdatesEvents(ConsumerRecord<String, ExtendedEventValue<AppointmentMetadata>> consumerRecord) {

        var value = consumerRecord.value();
        log.info("Received recover confirmation type event: {}", value.getTraceId());

        if(value instanceof AppointmentEventValue eventValue){
            String originalTopic = getOriginalTopic.apply(eventValue);

            eventPublisher.publishEvent(eventMapperToEmail.appointmentStatusUpdatesEmail(eventValue, originalTopic));
        }
        else if(value instanceof AppointmentErrorEventValue errorEventValue) {
            String originalTopic = getOriginalTopic.apply(errorEventValue);

            eventPublisher.publishEvent(eventMapperToEmail.appointmentStatusUpdatesEmail(errorEventValue,originalTopic));
        }

    }


}
