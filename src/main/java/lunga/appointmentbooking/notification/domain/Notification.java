package lunga.appointmentbooking.notification.domain;

import org.apache.commons.lang3.StringUtils;

public class Notification {

    public static final String REGISTRATION_EVENT = "registration-event";
    public static final String COMPLETE_REGISTRATION_EVENT = "complete-registration-event";
    public static final String EMAIL_VERIFIED_EVENT = "email-verified-event";
    public static final String DELETE_USER_ACCOUNT_EVENT = "deleted-user-account-event";
    public static final String PASSWORD_UPDATED_EVENT = "password-updated-event";
    public static final String DELETE_USER_ACCOUNT_REQUEST_EVENT = "deleted-user-account-request-event";
    public static final String PASSWORD_RESET_REQUEST_EVENT = "password-reset-request-event";

    public static String OTPEmailPattern() {

        return ("(" + REGISTRATION_EVENT + "|" + DELETE_USER_ACCOUNT_REQUEST_EVENT + "|"
                + PASSWORD_RESET_REQUEST_EVENT + ")(\\.retry)?");
    }

    public static String confirmationEmailPattern() {

        return ("(" + COMPLETE_REGISTRATION_EVENT + "|" + DELETE_USER_ACCOUNT_EVENT + "|"
                + PASSWORD_UPDATED_EVENT + ")(\\.retry)?");
    }

    public enum EventType {
        REGISTRATION_EVENT(Notification.REGISTRATION_EVENT),
        COMPLETE_REGISTRATION_EVENT(Notification.COMPLETE_REGISTRATION_EVENT),
        EMAIL_VERIFIED_EVENT(Notification.EMAIL_VERIFIED_EVENT),
        DELETE_ACCOUNT_EVENT(Notification.DELETE_USER_ACCOUNT_EVENT),
        DELETE_ACCOUNT_REQUEST_EVENT(Notification.DELETE_USER_ACCOUNT_REQUEST_EVENT),
        PASSWORD_RESET_REQUEST_EVENT(Notification.PASSWORD_RESET_REQUEST_EVENT),
        PASSWORD_UPDATED_EVENT(Notification.PASSWORD_UPDATED_EVENT);
        final String topic;

        EventType(String topic) {
            this.topic = topic;
        }

        public String getTopic() {
            return topic;
        }

        public static EventType fromTopic(String topic) {

            if (StringUtils.isBlank(topic)) return null;

            topic = topic.toLowerCase();

            for (EventType eventType : EventType.values()) {
                if (eventType.getTopic().equals(topic)) {
                    return eventType;
                }
            }
            return null;
        }
    }

}
