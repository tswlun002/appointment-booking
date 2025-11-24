package lunga.appointmentbooking.user.app;

import lunga.appointmentbooking.utils.Password;

public record DeleteAccountDTO(
        @Password String password
) {
}
