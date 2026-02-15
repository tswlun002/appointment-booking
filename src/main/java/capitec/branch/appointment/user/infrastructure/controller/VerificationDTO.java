package capitec.branch.appointment.user.infrastructure.controller;

import capitec.branch.appointment.utils.CustomerEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerificationDTO(
        @CustomerEmail String email,
        @NotBlank
        String otp,
        @NotNull
        Boolean isCapitecClient
) {
}
