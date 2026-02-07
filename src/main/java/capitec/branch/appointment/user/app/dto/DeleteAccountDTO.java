package capitec.branch.appointment.user.app.dto;

import capitec.branch.appointment.utils.Password;

public record DeleteAccountDTO(
        @Password String password
) {
}
