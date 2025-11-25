package capitec.branch.appointment.user.app;

import capitec.branch.appointment.utils.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterDTO(
        @NotBlank
        @Email
         String email,
         @NotBlank
         @Pattern(regexp = "[a-zA-Z]{2,}")
         String firstname,
         @NotBlank
        @Pattern(regexp = "[a-zA-Z]{2,}")
         String lastname,
         @Password
         String password

) {
}
