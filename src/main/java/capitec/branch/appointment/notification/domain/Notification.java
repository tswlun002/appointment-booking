package capitec.branch.appointment.notification.domain;

import org.apache.commons.lang3.StringUtils;

public class Notification {
    //---------------------------User --------------------------------//
    public static final String REGISTRATION_EVENT = "registration-event";
    public static final String COMPLETE_REGISTRATION_EVENT = "complete-registration-event";
    public static final String EMAIL_VERIFIED_EVENT = "email-verified-event";
    public static final String DELETE_USER_ACCOUNT_EVENT = "deleted-user-account-event";
    public static final String PASSWORD_UPDATED_EVENT = "password-updated-event";
    public static final String DELETE_USER_ACCOUNT_REQUEST_EVENT = "deleted-user-account-request-event";
    public static final String PASSWORD_RESET_REQUEST_EVENT = "password-reset-request-event";

    //---------------------------Appointments --------------------------------//
    public static final String APPOINTMENT_RESCHEDULED = "appointment-rescheduled";
    public static final String APPOINTMENT_CANCELED = "appointment-canceled";
    public static final String ATTENDED_APPOINTMENT = "attended-appointment";
    public static final String APPOINTMENT_BOOKED = "appointment-booked";


    public enum UserEventType {
        REGISTRATION_EVENT(Notification.REGISTRATION_EVENT),
        COMPLETE_REGISTRATION_EVENT(Notification.COMPLETE_REGISTRATION_EVENT),
        EMAIL_VERIFIED_EVENT(Notification.EMAIL_VERIFIED_EVENT),
        DELETE_ACCOUNT_EVENT(Notification.DELETE_USER_ACCOUNT_EVENT),
        DELETE_ACCOUNT_REQUEST_EVENT(Notification.DELETE_USER_ACCOUNT_REQUEST_EVENT),
        PASSWORD_RESET_REQUEST_EVENT(Notification.PASSWORD_RESET_REQUEST_EVENT),
        PASSWORD_UPDATED_EVENT(Notification.PASSWORD_UPDATED_EVENT);
        final String topic;

        UserEventType(String topic) {
            this.topic = topic;
        }

        public String getTopic() {
            return topic;
        }

        public static UserEventType fromTopic(String topic) {

            if (StringUtils.isBlank(topic)) return null;

            topic = topic.toLowerCase();

            for (UserEventType userEventType : UserEventType.values()) {
                if (userEventType.getTopic().equals(topic)) {
                    return userEventType;
                }
            }
            return null;
        }
    }
    public enum AppointmentEventType {
        APPOINTMENT_RESCHEDULED(Notification.APPOINTMENT_RESCHEDULED),
        APPOINTMENT_CANCELED(Notification.APPOINTMENT_CANCELED),
        ATTENDED_APPOINTMENT(Notification.ATTENDED_APPOINTMENT),
        APPOINTMENT_BOOKED(Notification.APPOINTMENT_BOOKED);
        final String topic;

        AppointmentEventType(String topic) {
            this.topic = topic;
        }

        public String getTopic() {
            return topic;
        }

        public static AppointmentEventType fromTopic(String topic) {

            if (StringUtils.isBlank(topic)) return null;

            topic = topic.toLowerCase();

            for (AppointmentEventType eventType : AppointmentEventType.values()) {
                if (eventType.getTopic().equals(topic)) {
                    return eventType;
                }
            }
            return null;
        }
    }

}
