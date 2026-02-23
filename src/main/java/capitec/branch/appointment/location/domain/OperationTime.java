package capitec.branch.appointment.location.domain;

import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalTime;

public record OperationTime(
        LocalTime openAt,
        LocalTime closeAt,
        boolean closed,
        boolean isHoliday,
        LocalDate day
) {

    public OperationTime(LocalTime openAt, LocalTime closeAt, boolean closed, boolean isHoliday, LocalDate day) {

        if (!closed) {
            Assert.notNull(openAt, "openAt");
            Assert.notNull(closeAt, "closeAt");
            Assert.isTrue(openAt.isBefore(closeAt), "openAt must be before closeAt");
            Assert.notNull(day, "fromDay");
        }

        this.openAt = openAt;
        this.closeAt = closeAt;
        this.closed = closed;
        this.isHoliday = isHoliday;
        this.day = day;
    }
}
