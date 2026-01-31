package capitec.branch.appointment.user.infrastructure.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerificationDTO(
        @NotBlank
        @Email String email,
        @NotBlank
        String otp,
        @NotNull
        Boolean isCapitecClient
) {
}
