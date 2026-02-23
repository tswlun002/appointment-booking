package capitec.branch.appointment.branch.app.port;

import java.time.LocalTime;

public record OperationHourDetails(
        LocalTime openTime,
        LocalTime closingTime,
        boolean closed
) {
}
