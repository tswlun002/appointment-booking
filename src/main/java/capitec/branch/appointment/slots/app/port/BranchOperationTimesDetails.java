package capitec.branch.appointment.slots.app.port;
import java.time.LocalDate;

import java.util.Map;
import java.util.Objects;

public record BranchOperationTimesDetails(
        String branchId,
        Map<LocalDate, OperationTimesDetails> operationTimes,
        Map<LocalDate, AppointmentInfoDetails> appointmentInfo

        ) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BranchOperationTimesDetails that)) return false;
        return Objects.equals(branchId, that.branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(branchId);
    }
}
