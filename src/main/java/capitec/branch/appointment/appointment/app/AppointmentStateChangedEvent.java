package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.Email;
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
        AppointmentStatus fromState,
        @NotNull
        AppointmentStatus toState,
        String triggeredBy,
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
            String branchName,
            Address address
    ) {
        return new AppointmentStateChangedEvent(
                appointmentId,
                appointmentReference,
                customerUsername,
                null,
                AppointmentStatus.BOOKED,
                customerUsername,
                LocalDateTime.now(),
                Map.of(
                        "day", day,
                        "startTime", startTime,
                        "endTime", endTime,
                        "branchName", branchName,
                        "address", address
                )
        );
    }

    public static AppointmentStateChangedEvent transition(
            UUID appointmentId,
            String appointmentReference,
            String customerUsername,
            AppointmentStatus fromState,
            AppointmentStatus toState,
            String triggeredBy,
            Map<String, Object> metadata
    ) {
        return new AppointmentStateChangedEvent(
                appointmentId,
                appointmentReference,
                customerUsername,
                fromState,
                toState,
                triggeredBy,
                LocalDateTime.now(),
                metadata != null ? metadata : Map.of()
        );
    }
}
