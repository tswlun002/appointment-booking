package capitec.branch.appointment.event.infrastructure.kafka.consumer;


import capitec.branch.appointment.notification.domain.*;
import capitec.branch.appointment.sharekernel.event.metadata.AppointmentMetadata;
import capitec.branch.appointment.sharekernel.event.metadata.OTPMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;


@Mapper(componentModel = "spring")
public interface EventMapperToEmail {

    //---------------------------------------User Email---------------------------------------
    @Mapping(source = "event.fullname",target = "fullname")
    @Mapping(source = "event.email", target = "email")
    @Mapping(source = "event.otpCode", target = "OTPCode")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToEventType")
    OTPEmail eventmapToOTPEmail(OTPMetadata event, String topic, String traceId);

    @Mapping(source = "event.fullname",target = "fullname")
    @Mapping(source = "event.email", target = "email")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToEventType")
    ConfirmationEmail eventToConfirmationEmail(OTPMetadata event, String topic, String traceId);

    //---------------------------------------Appointment Email---------------------------------------
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.otherData", target = "day", qualifiedByName = "appointmentDay")
    @Mapping(source = "event.otherData", target = "startTime", qualifiedByName = "appointmentStartTime")
    @Mapping(source = "event.otherData", target = "endTime", qualifiedByName = "appointmentEndTime")
    @Mapping(source = "event.branchId", target = "branchId")
    AppointmentBookedEmail appointmentBookedEmail(AppointmentMetadata event, String topic, String traceId);
    @Mapping(target = "fullname", ignore = true)
    @Mapping( target = "email", ignore = true)
    @Mapping(source = "event.customerUsername", target = "customerUsername")
    @Mapping(source = "event.branchId", target = "branchId")
    @Mapping(source = "event.reference", target = "reference")
    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "topic", target = "eventType",qualifiedByName = "topicToAppointmentEventType")
    @Mapping(source = "event.otherData", target = "fromState", qualifiedByName = "appointmentFromState")
    @Mapping(source = "event.otherData", target = "toState", qualifiedByName = "appointmentToState")
    @Mapping(source = "event.createdAt", target = "createdAt")
    @Mapping(source = "event.otherData", target = "triggeredBy", qualifiedByName = "appointmentUpdateTriggeredBy")
    AppointmentStatusUpdatesEmail appointmentStatusUpdatesEmail(AppointmentMetadata event, String topic, String traceId);

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
        return otherData.get("triggeredBy") != null ? otherData.get("triggeredBy").toString() : null;
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
        Object toState = otherData.get("toState");
        if (toState != null) {
            return toState.toString();
        } else {
            return null;
        }
    }

}
