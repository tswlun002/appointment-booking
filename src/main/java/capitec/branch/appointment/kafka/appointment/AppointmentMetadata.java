package capitec.branch.appointment.kafka.appointment;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


public record AppointmentMetadata(
        UUID id,
        String reference,
        String branchId,
        String customerUsername,
        LocalDateTime createdAt,
        Map<String, Object> otherData


) implements Serializable {

}
