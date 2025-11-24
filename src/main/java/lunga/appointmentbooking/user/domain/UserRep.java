package lunga.appointmentbooking.user.domain;

import lunga.appointmentbooking.utils.Username;
import lunga.appointmentbooking.utils.Name;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRep(@Username long id, @NotBlank @Email String email,
                      @Name String firstname, @Name String lastname, boolean enabled, boolean verified
    ){
}
