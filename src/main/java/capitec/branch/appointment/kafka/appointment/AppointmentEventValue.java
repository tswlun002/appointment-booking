package capitec.branch.appointment.kafka.appointment;


import capitec.branch.appointment.kafka.domain.ExtendedEventValue;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public interface AppointmentEventValue extends ExtendedEventValue<AppointmentMetadata> {
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
