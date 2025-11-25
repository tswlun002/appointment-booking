package capitec.branch.appointment.user.domain;

import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Name;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRep(@Username long id, @NotBlank @Email String email,
                      @Name String firstname, @Name String lastname, boolean enabled, boolean verified
    ){
}
