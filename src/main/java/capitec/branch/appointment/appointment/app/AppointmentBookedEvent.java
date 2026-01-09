package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentBookedEvent(
        @NotBlank
        String appointmentReference,
        @Username
        String username,
        @Email
        String email,
        @NotNull
        LocalDate day,
        @NotNull
        LocalTime startTime,
        @NotNull
        LocalTime endTime,
        @NotBlank
        String branchName,
        @NotNull
        Address address

) {
}
