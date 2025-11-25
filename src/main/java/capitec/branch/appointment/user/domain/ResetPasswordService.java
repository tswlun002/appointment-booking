package capitec.branch.appointment.user.domain;

import jakarta.validation.Valid;

public interface ResetPasswordService {

    boolean passwordReset( @Valid User user);
}
