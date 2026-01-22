package capitec.branch.appointment.branch.domain.operationhours;

import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalTime;

public record OperationHoursOverride (
        LocalDate effectiveDate,
        LocalTime openTime,
        LocalTime closingTime,
        boolean closed,
        String reason
){

    public OperationHoursOverride {
        Assert.notNull(effectiveDate, "Effective date cannot be null");
        Assert.notNull(openTime, "Open time cannot be null");
        Assert.notNull(closingTime, "Closing time cannot be null");
        Assert.isTrue(openTime.isBefore(closingTime), "Open time must be before closing time");
        Assert.isTrue(!closed || reason != null, "Closed operation hours must have a reason");
        Assert.hasText(reason, "Closed operation hours must have a reason");
        Assert.isTrue(effectiveDate.isEqual(LocalDate.now()) || effectiveDate.isAfter(LocalDate.now()), "Effective date must not be in the past");
    }
    public boolean isExpired() {
        return effectiveDate.isBefore(LocalDate.now());
    }

}
