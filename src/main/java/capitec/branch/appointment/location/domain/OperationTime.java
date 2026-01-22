package capitec.branch.appointment.location.domain;

import org.springframework.util.Assert;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OperationTime(
        LocalTime openTime,
        LocalTime closingTime,
        boolean closed,
        DayOfWeek fromDay,
        DayOfWeek toDay
) {

    public OperationTime {
        Assert.notNull(openTime, "openTime");
        Assert.notNull(closingTime, "closingTime");
        Assert.isTrue(openTime.isBefore(closingTime), "openTime must be before closingTime");
        Assert.notNull(fromDay, "fromDay");}
}
