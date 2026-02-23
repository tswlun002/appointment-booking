package capitec.branch.appointment.authentication.infrastructure.controller;

import capitec.branch.appointment.utils.Password;
import capitec.branch.appointment.utils.Username;

public record UserCredentialDTO(
        @Username
        String username,
        @Password
        String Password
) {
}
