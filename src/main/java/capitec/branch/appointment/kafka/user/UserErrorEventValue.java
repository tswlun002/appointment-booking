package capitec.branch.appointment.kafka.user;

import capitec.branch.appointment.kafka.domain.ExtendedErrorEventValue;

public interface UserErrorEventValue extends ExtendedErrorEventValue<UserMetadata> {

    default String getFullname() {
        return getMetadata().fullname();
    }

    default String getEmail() {
        return getMetadata().email();
    }

    default String getUsername() {
        return getMetadata().username();
    }
}