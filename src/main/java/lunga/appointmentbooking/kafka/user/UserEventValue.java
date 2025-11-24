package lunga.appointmentbooking.kafka.user;


import lunga.appointmentbooking.kafka.domain.EventValue;

public interface UserEventValue extends EventValue {
    String getFullname();
    String getEmail();

    String getUsername();
}
