package capitec.branch.appointment.kafka.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;


public record UserMetadata(

        @NotBlank(message = "Event name is required")
        String fullname,
        @NotBlank(message = "username is required")
        String username,
        @NotBlank(message = "Email is required")
        @Email
        String email
) implements Serializable {

}
