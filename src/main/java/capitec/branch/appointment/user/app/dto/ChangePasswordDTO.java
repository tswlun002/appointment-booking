package capitec.branch.appointment.user.app.dto;


import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Password;

public record ChangePasswordDTO(
      @Username String username,
       @Password String newPassword,
        String OTP
) {
}
