package capitec.branch.appointment.event.infrastructure.kafka.consumer;


import capitec.branch.appointment.kafka.appointment.AppointmentErrorEventValue;
import capitec.branch.appointment.kafka.appointment.AppointmentEventValue;
import capitec.branch.appointment.kafka.user.UserErrorEventValue;
import capitec.branch.appointment.kafka.user.UserEventValue;
import capitec.branch.appointment.notification.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;


@Mapper(componentModel = "spring")
public interface EventMapperToEmail {

    //---------------------------------------User Email---------------------------------------
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


    //---------------------------------------Appointment Email---------------------------------------
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "event.topic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.metadata.otherData", target = "day", qualifiedByName = "appointmentDay")
    @Mapping(source = "event.metadata.otherData", target = "startTime", qualifiedByName = "appointmentStartTime")
    @Mapping(source = "event.metadata.otherData", target = "endTime", qualifiedByName = "appointmentEndTime")
    @Mapping(source = "event.branchId", target = "branchId")
    AppointmentBookedEmail appointmentBookedEmail(AppointmentEventValue event);
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "originalTopic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.metadata.otherData", target = "day", qualifiedByName = "appointmentDay")
    @Mapping(source = "event.metadata.otherData", target = "startTime", qualifiedByName = "appointmentStartTime")
    @Mapping(source = "event.metadata.otherData.", target = "endTime", qualifiedByName = "appointmentEndTime")
    @Mapping(source = "event.branchId", target = "branchId")
    AppointmentBookedEmail appointmentBookedEmail(AppointmentEventValue event,String originalTopic);
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "event.topic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.metadata.otherData", target = "day", qualifiedByName = "appointmentDay")
    @Mapping(source = "event.metadata.otherData", target = "startTime", qualifiedByName = "appointmentStartTime")
    @Mapping(source = "event.metadata.otherData", target = "endTime", qualifiedByName = "appointmentEndTime")
    @Mapping(source = "event.branchId", target = "branchId")
    AppointmentBookedEmail appointmentBookedEmail(AppointmentErrorEventValue event);
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "originalTopic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.metadata.otherData", target = "day", qualifiedByName = "appointmentDay")
    @Mapping(source = "event.metadata.otherData", target = "startTime", qualifiedByName = "appointmentStartTime")
    @Mapping(source = "event.metadata.otherData", target = "endTime", qualifiedByName = "appointmentEndTime")
    @Mapping(source = "event.branchId", target = "branchId")
    AppointmentBookedEmail appointmentBookedEmail(AppointmentErrorEventValue event, String originalTopic);
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.branchId", target = "branchId")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "event.topic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.metadata.otherData", target = "fromState", qualifiedByName = "appointmentFromState")
    @Mapping(source = "event.metadata.otherData", target = "toState", qualifiedByName = "appointmentToState")
    @Mapping(source = "event.createdAt", target = "createdAt")
    @Mapping(source = "event.metadata.otherData", target = "triggeredBy", qualifiedByName = "appointmentUpdateTriggeredBy")
    AppointmentStatusUpdatesEmail appointmentStatusUpdatesEmail(AppointmentEventValue event);
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.branchId", target = "branchId")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "originalTopic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.metadata.otherData", target = "fromState", qualifiedByName = "appointmentFromState")
    @Mapping(source = "event.metadata.otherData", target = "toState", qualifiedByName = "appointmentToState")
    @Mapping(source = "event.createdAt", target = "createdAt")
    @Mapping(source = "event.metadata.otherData", target = "triggeredBy", qualifiedByName = "appointmentUpdateTriggeredBy")
    AppointmentStatusUpdatesEmail appointmentStatusUpdatesEmail(AppointmentEventValue event,String originalTopic);
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.branchId", target = "branchId")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "event.topic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.metadata.otherData", target = "fromState", qualifiedByName = "appointmentFromState")
    @Mapping(source = "event.metadata.otherData", target = "toState", qualifiedByName = "appointmentToState")
    @Mapping(source = "event.createdAt", target = "createdAt")
    @Mapping(source = "event.metadata.otherData", target = "triggeredBy", qualifiedByName = "appointmentUpdateTriggeredBy")
    AppointmentStatusUpdatesEmail appointmentStatusUpdatesEmail(AppointmentErrorEventValue event);
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.branchId", target = "branchId")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "event.traceId", target = "traceId")
    @Mapping(source = "originalTopic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.metadata.otherData", target = "fromState", qualifiedByName = "appointmentFromState")
    @Mapping(source = "event.metadata.otherData", target = "toState", qualifiedByName = "appointmentToState")
    @Mapping(source = "event.createdAt", target = "createdAt")
    @Mapping(source = "event.metadata.otherData", target = "triggeredBy", qualifiedByName = "appointmentUpdateTriggeredBy")
    AppointmentStatusUpdatesEmail appointmentStatusUpdatesEmail(AppointmentErrorEventValue event, String originalTopic);

    @Named("topicToEventType")
    default Notification.UserEventType topicToEventType(String topic) {

        return Notification.UserEventType.fromTopic(topic);
    }
    @Named("topicToAppointmentEventType")
    default Notification.AppointmentEventType topicToAppointmentEventType(String topic) {

        return Notification.AppointmentEventType.fromTopic(topic);
    }
    @Named("appointmentUpdateTriggeredBy")
    default String appointmentUpdateTriggeredBy(Map<String,Object> otherData) {
        if(otherData ==null || otherData.isEmpty()) return null;
        return otherData.get("triggeredBy") != null ? otherData.get("day").toString() : null;
    }
    @Named("appointmentDay")
    default LocalDate appointmentDay(Map<String,Object> otherData) {
        if(otherData ==null || otherData.isEmpty()) return null;
        return otherData.get("day") != null ? LocalDate.parse(otherData.get("day").toString()) : null;
    }
    @Named("appointmentStartTime")
    default LocalTime appointmentStartTime(Map<String,Object> otherData) {
        if(otherData ==null || otherData.isEmpty()) return null;
        Object startTime = otherData.get("startTime");
        if (startTime != null) {
            String string = startTime.toString();
            return LocalTime.parse(string);
        } else {
            return null;
        }
    }
    @Named("appointmentEndTime")
    default LocalTime appointmentEndTime(Map<String,Object> otherData) {
        if(otherData ==null || otherData.isEmpty()) return null;
        Object endTime = otherData.get("endTime");
        if (endTime != null) {
            String string = endTime.toString();
            return LocalTime.parse(string);
        } else {
            return null;
        }
    }

    @Named("appointmentFromState")
    default String appointmentFromState(Map<String,Object> otherData) {
        if(otherData ==null || otherData.isEmpty()) return null;
        Object endTime = otherData.get("fromState");
        if (endTime != null) {
            return endTime.toString();
        } else {
            return null;
        }
    }
    @Named("appointmentToState")
    default String appointmentToState(Map<String,Object> otherData) {
        if(otherData ==null || otherData.isEmpty()) return null;
        Object endTime = otherData.get("toDate");
        if (endTime != null) {
            return endTime.toString();
        } else {
            return null;
        }
    }


//    @Named("userMetadataToFullname")
//    default String userMetadataToFullname(AppointmentMetadata userMetadata) {
//
//        return userMetadata.fullname();
//    }
//    @Named("userMetadataToEmail")
//    default String userMetadataToEmail(AppointmentMetadata userMetadata) {
//
//        return userMetadata.email();
//    }
}
