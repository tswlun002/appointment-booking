package capitec.branch.appointment.event.app;

public class Topics {

    public static final String REGISTRATION_EVENT = "registration-event";
    public static final String COMPLETE_REGISTRATION_EVENT = "complete-registration-event";
    public static final String EMAIL_VERIFIED_EVENT = "email-verified-event";
    public  static final String DELETE_USER_ACCOUNT_EVENT = "deleted-user-account-event";
    public  static final String  PASSWORD_UPDATED_EVENT = "password-updated-event";
    public  static final String  DELETE_USER_ACCOUNT_REQUEST_EVENT = "deleted-user-account-request-event";
    public  static final String  PASSWORD_RESET_REQUEST_EVENT = "password-reset-request-event";
    public static final String APPOINTMENT_RESCHEDULED = "appointment-rescheduled";
    public static final String APPOINTMENT_CANCELED = "appointment-canceled";
    public static final String ATTENDED_APPOINTMENT = "attended-appointment";
    public static final String APPOINTMENT_BOOKED = "appointment-booked";

    public static String OTPEmailPattern() {

        return ("(" + REGISTRATION_EVENT + "|" + DELETE_USER_ACCOUNT_REQUEST_EVENT + "|"
                + PASSWORD_RESET_REQUEST_EVENT + ")\\.retry");
    }

    public static String confirmationEmailPattern() {

        return ("(" + COMPLETE_REGISTRATION_EVENT + "|" + DELETE_USER_ACCOUNT_EVENT + "|"
                + PASSWORD_UPDATED_EVENT + ")\\.retry");
    }

    public static String bookedAppointmentPattern() {

        return ("(" + APPOINTMENT_RESCHEDULED + "|" + APPOINTMENT_BOOKED + ")\\.retry");
    }
    public static String appointmentUpdatesPattern() {

        return ("(" + APPOINTMENT_CANCELED + "|" + ATTENDED_APPOINTMENT + ")\\.retry");
    }
}
