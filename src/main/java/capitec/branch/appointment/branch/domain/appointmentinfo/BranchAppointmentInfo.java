package capitec.branch.appointment.branch.domain.appointmentinfo;

import jakarta.validation.constraints.NotNull;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDate;

public record BranchAppointmentInfo(
        @NotNull
        Duration slotDuration,
        @NotNull
        double utilizationFactor,
        int staffCount,
        @NotNull
        LocalDate day
) {

    public BranchAppointmentInfo(Duration slotDuration, double utilizationFactor, int staffCount, LocalDate day) {

        Assert.notNull(slotDuration, "Slot duration cannot be null");
        Assert.notNull(day, "Day cannot be null");
        Assert.isTrue(slotDuration.toMinutes() > 0, "Slot duration must be greater than 0 minutes");

        this.slotDuration = slotDuration;
        this.utilizationFactor = utilizationFactor;
        this.day = day;
        this.staffCount = staffCount;

    }
}
