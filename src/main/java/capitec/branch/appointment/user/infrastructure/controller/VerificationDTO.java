package capitec.branch.appointment.user.infrastructure.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerificationDTO(
        @NotBlank
        @Email String email,
        String otp
) {
}
