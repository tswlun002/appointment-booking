package capitec.branch.appointment.location.domain;

import org.springframework.util.Assert;
import java.time.LocalDate;
import java.time.LocalTime;

public record OperationTime(
        LocalTime openAt,
        LocalTime closeAt,
        boolean closed,
        boolean isHoliday,
        LocalDate fromDay,
        LocalDate toDay
) {

    public OperationTime(LocalTime openAt, LocalTime closeAt, boolean closed, boolean isHoliday, LocalDate fromDay, LocalDate toDay) {

        if (!closed) {
            Assert.notNull(openAt, "openAt");
            Assert.notNull(closeAt, "closeAt");
            Assert.isTrue(openAt.isBefore(closeAt), "openAt must be before closeAt");
            Assert.notNull(fromDay, "fromDay");
        }

        this.openAt = openAt;
        this.closeAt = closeAt;
        this.closed = closed;
        this.isHoliday = isHoliday;
        this.fromDay = fromDay;
        this.toDay = toDay;
    }
}
