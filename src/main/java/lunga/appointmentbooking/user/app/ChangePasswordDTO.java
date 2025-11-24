package lunga.appointmentbooking.user.app;


import lunga.appointmentbooking.utils.Username;
import lunga.appointmentbooking.utils.Password;

public record ChangePasswordDTO(
      @Username String username,
       @Password String newPassword,
        String OTP
) {
}
