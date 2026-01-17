package capitec.branch.appointment.kafka.appointment;

import capitec.branch.appointment.kafka.domain.ExtendedErrorEventValue;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public interface AppointmentErrorEventValue extends ExtendedErrorEventValue<AppointmentMetadata> {

    default UUID getId() {
        return getMetadata().id();
    }

    default String getBranchId() {
        return getMetadata().branchId();
    }

    default String getCustomerUsername() {
        return getMetadata().customerUsername();
    }

    default LocalDateTime getCreatedAt(){
        return getMetadata().createdAt();
    }

    default String getReference(){
        return getMetadata().reference();
    }
}