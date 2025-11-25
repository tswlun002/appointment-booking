package capitec.branch.appointment.notification.infrastructure.kafka;


import capitec.branch.appointment.kafka.user.UserDefaultErrorEvent;
import capitec.branch.appointment.kafka.user.UserDefaultEvent;
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
    OTPEmail userEventmapToOTPEmail(UserDefaultEvent event);

    @Mapping(source = "fullname",target = "fullname")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "value", target = "OTPCode")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToEventType")
    OTPEmail userEventmapToOTPEmail(UserDefaultErrorEvent event);

    @Mapping(source = "fullname",target = "fullname")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToEventType")
    ConfirmationEmail userEventMapToConfirmationEmail(UserDefaultEvent event);

    @Mapping(source = "fullname",target = "fullname")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToEventType")
    OTPEmail userEventMapToConfirmationEmail(UserDefaultErrorEvent event);

    @Named("topicToEventType")
    default Notification.EventType topicToEventType(String topic) {

        return Notification.EventType.fromTopic(topic);
    }
}
