package capitec.branch.appointment.notification.infrastructure.kafka;


import capitec.branch.appointment.kafka.user.UserErrorEventValue;
import capitec.branch.appointment.kafka.user.UserEventValue;
import capitec.branch.appointment.notification.domain.ConfirmationEmail;
import capitec.branch.appointment.notification.domain.Notification;
import capitec.branch.appointment.notification.domain.OTPEmail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;



@Mapper(componentModel = "spring")
public interface EventMapperToEmail {

    @Mapping(source = "fullname",target = "fullname")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "value", target = "OTPCode")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToEventType")
    OTPEmail userEventmapToOTPEmail(UserEventValue event);

    @Mapping(source = "event.fullname",target = "fullname")
    @Mapping(source = "event.email", target = "email")
    @Mapping(source = "event.value", target = "OTPCode")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "originalTopic", target = "eventType",qualifiedByName = "topicToEventType")
    OTPEmail userEventmapToOTPEmail(UserEventValue event, String originalTopic);

    @Mapping(source = "fullname",target = "fullname")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "value", target = "OTPCode")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToEventType")
    OTPEmail userEventmapToOTPEmail(UserErrorEventValue event);

    @Mapping(source = "event.fullname",target = "fullname")
    @Mapping(source = "event.email", target = "email")
    @Mapping(source = "event.value", target = "OTPCode")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "originalTopic", target = "eventType",qualifiedByName = "topicToEventType")
    OTPEmail userEventmapToOTPEmail(UserErrorEventValue event, String originalTopic);

    @Mapping(source = "fullname",target = "fullname")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToEventType")
    ConfirmationEmail userEventMapToConfirmationEmail(UserEventValue event);
    @Mapping(source = "event.fullname",target = "fullname")
    @Mapping(source = "event.email", target = "email")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "originalTopic", target = "eventType",qualifiedByName = "topicToEventType")
    ConfirmationEmail userEventMapToConfirmationEmail(UserEventValue event,String originalTopic);

    @Mapping(source = "fullname",target = "fullname")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToEventType")
    OTPEmail userEventMapToConfirmationEmail(UserErrorEventValue event);
    @Mapping(source = "event.fullname",target = "fullname")
    @Mapping(source = "event.email", target = "email")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "originalTopic", target = "eventType",qualifiedByName = "topicToEventType")
    OTPEmail userEventMapToConfirmationEmail(UserErrorEventValue event, String originalTopic);

    @Named("topicToEventType")
    default Notification.EventType topicToEventType(String topic) {

        return Notification.EventType.fromTopic(topic);
    }


//    @Named("userMetadataToFullname")
//    default String userMetadataToFullname(UserMetadata userMetadata) {
//
//        return userMetadata.fullname();
//    }
//    @Named("userMetadataToEmail")
//    default String userMetadataToEmail(UserMetadata userMetadata) {
//
//        return userMetadata.email();
//    }
}
