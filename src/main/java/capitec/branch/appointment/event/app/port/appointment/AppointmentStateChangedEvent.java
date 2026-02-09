package capitec.branch.appointment.event.app.port.appointment;

import capitec.branch.appointment.sharekernel.EventTrigger;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

public record AppointmentStateChangedEvent(
        @NotNull
        UUID appointmentId,
        @NotBlank
        String appointmentReference,
        @Username
        String customerUsername,
        @NotBlank
        String branchId,
        String fromState,
        @NotNull
        String  toState,
        EventTrigger triggeredBy,
        @NotNull
        LocalDateTime occurredAt,
        Map<String, Object> metadata
) {

    public static AppointmentStateChangedEvent booked(
            UUID appointmentId,
            String appointmentReference,
            String customerUsername,
            LocalDate day,
            LocalTime startTime,
            LocalTime endTime,
            String branchId
    ) {
        return new AppointmentStateChangedEvent(
                appointmentId,
                appointmentReference,
                customerUsername,
                branchId,
                null,
                "BOOKED",
                EventTrigger.CUSTOMER,
                LocalDateTime.now(),
                Map.of(
                        "day", day,
                        "startTime", startTime,
                        "endTime", endTime
                )
        );
    }

    public static AppointmentStateChangedEvent transition(
            UUID appointmentId,
            String appointmentReference,
            String customerUsername,
            String branchId,
            String fromState,
            String toState,
            EventTrigger triggeredBy,
            Map<String, Object> metadata
    ) {
        return new AppointmentStateChangedEvent(
                appointmentId,
                appointmentReference,
                customerUsername,
                branchId,
                fromState,
                toState,
                triggeredBy,
                LocalDateTime.now(),
                metadata != null ? metadata : Map.of()
        );
    }
}
