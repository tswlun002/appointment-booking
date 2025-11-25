package capitec.branch.appointment.event.domain;

import capitec.branch.appointment.kafka.domain.DeadLetterService;
import capitec.branch.appointment.kafka.domain.ErrorEventValue;

import java.util.Set;

public interface UserDeadLetterService extends DeadLetterService<ErrorEventValue>  {
    void saveDeadLetter(ErrorEventValue errorEventValue);
    Set<ErrorEventValue> findByStatus(RecordStatus recordStatus );

    void updateStatus( ErrorEventValue errorEventValue);
}
