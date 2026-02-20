package capitec.branch.appointment.branch.domain.operationhours;

import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalTime;

public record OperationHoursOverride (
        LocalDate effectiveDate,
        LocalTime openAt,
        LocalTime closeAt,
        boolean closed,
        String reason
){

    public OperationHoursOverride {
        Assert.notNull(effectiveDate, "Effective date cannot be null");
        Assert.notNull(openAt, "Open time cannot be null");
        Assert.notNull(closeAt, "Closing time cannot be null");
        Assert.isTrue(openAt.isBefore(closeAt), "Open time must be before closing time");
        Assert.isTrue(!closed || reason != null, "Closed operation hours must have a reason");
        Assert.hasText(reason, "Closed operation hours must have a reason");
        if(effectiveDate !=null){
            Assert.isTrue( effectiveDate.isEqual(LocalDate.now()) || effectiveDate.isAfter(LocalDate.now()), "Effective date must not be in the past");
        }
    }
    public boolean isExpired() {
        return effectiveDate.isBefore(LocalDate.now());
    }

}
