package capitec.branch.appointment.kafka.appointment;

import capitec.branch.appointment.kafka.domain.ExtendedErrorEventValue;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public interface AppointmentErrorEventValue extends ExtendedErrorEventValue<AppointmentMetadata> {

    default UUID id() {
        return getMetadata().id();
    }

    default String branchId() {
        return getMetadata().branchId();
    }

    default String customerUsername() {
        return getMetadata().customerUsername();
    }

    default LocalDateTime createdAt(){
        return getMetadata().createdAt();
    }

    default String reference(){
        return getMetadata().reference();
    }
    default Map<String, Object> getOtherData(){return getMetadata().otherData();}
}