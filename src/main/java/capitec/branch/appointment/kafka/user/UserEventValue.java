package capitec.branch.appointment.kafka.user;


import capitec.branch.appointment.kafka.domain.EventValue;

public interface UserEventValue extends EventValue {
    String getFullname();
    String getEmail();

    String getUsername();
}
