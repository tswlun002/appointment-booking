package lunga.appointmentbooking.event.domain;

import lunga.appointmentbooking.kafka.domain.DeadLetterService;
import lunga.appointmentbooking.kafka.domain.ErrorEventValue;

import java.util.Set;

public interface UserDeadLetterService extends DeadLetterService<ErrorEventValue>  {
    void saveDeadLetter(ErrorEventValue errorEventValue);
    Set<ErrorEventValue> findByStatus(RecordStatus recordStatus );

    void updateStatus( ErrorEventValue errorEventValue);
}
