package capitec.branch.appointment.event.infrastructure.kafka.consumer;

import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.notification.domain.ConfirmationEmail;
import capitec.branch.appointment.notification.domain.OTPEmail;
import capitec.branch.appointment.utils.sharekernel.metadata.AppointmentMetadata;
import capitec.branch.appointment.utils.sharekernel.metadata.OTPMetadata;
import capitec.branch.appointment.notification.domain.AppointmentBookedEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventConsumerForEmail {

    private final ApplicationEventPublisher eventPublisher;
    private  final EventMapperToEmail eventMapperToEmail;

    private final Function<EventValue<String,?>,String>  getOriginalTopic =(eventValue)-> eventValue.topic().substring(0, eventValue.topic().lastIndexOf("."));

    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).REGISTRATION_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).DELETE_USER_ACCOUNT_REQUEST_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).PASSWORD_RESET_REQUEST_EVENT}",
    },
            groupId = "otp-events",autoStartup = "${kafka.listen.consumer.auto.start:true}")
    public  void OTPEvents(ConsumerRecord<String, EventValue<String, OTPMetadata>> consumerRecord)  {

        var metadata = consumerRecord.value();

        log.info("Received otp type event: {}", metadata.traceId());

        OTPEmail event = eventMapperToEmail.eventmapToOTPEmail(metadata.value(), metadata.topic(), metadata.traceId());
        eventPublisher.publishEvent(event);

    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.event.app.Topics).OTPEmailPattern()}",
            groupId = "otp-events",autoStartup = "${kafka.listen.consumer.auto.start:true}")
    public void recoveryOTPEvents(ConsumerRecord<String, EventValue<String, OTPMetadata>> consumerRecord) {

        var metadata = consumerRecord.value();
        String traceId = metadata.traceId();
        log.info("Received recover otp type event: {}", traceId);

        String originalTopic = getOriginalTopic.apply(metadata);

        eventPublisher.publishEvent(eventMapperToEmail.eventmapToOTPEmail(metadata.value(), originalTopic, traceId));


    }



    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).COMPLETE_REGISTRATION_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).DELETE_USER_ACCOUNT_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).PASSWORD_UPDATED_EVENT}",
    },
            groupId = "confirmation-events",autoStartup = "${kafka.listen.consumer.auto.start:true}")
    public void confirmationEvents(ConsumerRecord<String, EventValue<String, OTPMetadata>>  consumerRecord)  {

        var metadata = consumerRecord.value();

        String traceId = metadata.traceId();
        log.info("Received confirmation type event: {}", traceId);

        eventPublisher.publishEvent(eventMapperToEmail.eventToConfirmationEmail(metadata.value(), metadata.topic(), traceId));


    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.event.app.Topics).confirmationEmailPattern()}",
            groupId = "confirmation-events",autoStartup = "${kafka.listen.consumer.auto.start:true}")
    public void recoveryConfirmationEvents(ConsumerRecord<String, EventValue<String,OTPMetadata>> consumerRecord) {

        var metadata = consumerRecord.value();
        String traceId = metadata.traceId();
        log.info("Received recover confirmation type event: {}", traceId);
        String originalTopic = getOriginalTopic.apply(metadata);
        ConfirmationEmail event = eventMapperToEmail.eventToConfirmationEmail(metadata.value(), originalTopic, traceId);
        eventPublisher.publishEvent(event);

    }


    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).APPOINTMENT_RESCHEDULED}",
            "#{T(capitec.branch.appointment.event.app.Topics).APPOINTMENT_BOOKED}",
    },
            groupId = "booked-appointment-events",autoStartup = "${kafka.listen.consumer.auto.start:true}")
    public void bookedAppointmentEvents(ConsumerRecord<String, EventValue<String,AppointmentMetadata>> consumerRecord)  {

        var metadata = consumerRecord.value();

        String traceId = metadata.traceId();
        log.info("Received confirmation type event: {}", traceId);
        AppointmentBookedEmail event = eventMapperToEmail.appointmentBookedEmail(metadata.value(), metadata.topic(), traceId);
        eventPublisher.publishEvent(event);

    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.event.app.Topics).bookedAppointmentPattern()}",
            groupId = "booked-appointment-events",autoStartup = "${kafka.listen.consumer.auto.start:true}")
    public void recoveryBookedAppointmentEvents(ConsumerRecord<String, EventValue<String,AppointmentMetadata>> consumerRecord) {

        var metadata = consumerRecord.value();
        String traceId = metadata.traceId();
        log.info("Received recover confirmation type event: {}", traceId);
        String originalTopic = getOriginalTopic.apply(metadata);
        AppointmentBookedEmail event = eventMapperToEmail.appointmentBookedEmail(metadata.value(), originalTopic, traceId);
        eventPublisher.publishEvent(event);

    }

    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).APPOINTMENT_CANCELED}",
            "#{T(capitec.branch.appointment.event.app.Topics).ATTENDED_APPOINTMENT}"
    },
            groupId = "appointment-updates-events",autoStartup = "${kafka.listen.consumer.auto.start:true}")
    public void appointmentUpdatesEvents(ConsumerRecord<String, EventValue<String,AppointmentMetadata>> consumerRecord)  {

        var metadata = consumerRecord.value();

        String traceId = metadata.traceId();
        log.info("Received confirmation type event: {}", traceId);
        eventPublisher.publishEvent(eventMapperToEmail.appointmentStatusUpdatesEmail(metadata.value(), metadata.topic(), traceId));


    }
    @KafkaListener(topicPattern = "#{T(capitec.branch.appointment.event.app.Topics).appointmentUpdatesPattern()}",
            groupId = "appointment-updates-events",autoStartup = "${kafka.listen.consumer.auto.start:true}")
    public void recoveryAppointmentUpdatesEvents(ConsumerRecord<String, EventValue<String,AppointmentMetadata>> consumerRecord) {

        var metadata = consumerRecord.value();
        String traceId = metadata.traceId();
        log.info("Received recover confirmation type event: {}", traceId);
        String originalTopic = getOriginalTopic.apply(metadata);
        eventPublisher.publishEvent(eventMapperToEmail.appointmentStatusUpdatesEmail(metadata.value(), originalTopic, traceId));

    }


}
