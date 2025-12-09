package capitec.branch.appointment.branch.domain.appointmentinfo;

import capitec.branch.appointment.day.domain.DayType;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

public record BranchAppointmentInfo(
        @NotNull
        Duration slotDuration,
        @NotNull
        double utilizationFactor,
        int staffCount,
        @NotNull
        DayType dayType
) {

    public BranchAppointmentInfo(Duration slotDuration, double utilizationFactor, int staffCount,DayType dayType) {

        assert slotDuration != null;
        assert dayType != null;
        assert staffCount > 0;

        this.slotDuration = slotDuration;
        this.utilizationFactor = utilizationFactor;
        this.dayType = dayType;
        this.staffCount = staffCount;

    }
}
