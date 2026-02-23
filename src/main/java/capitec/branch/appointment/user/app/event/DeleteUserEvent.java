package capitec.branch.appointment.user.app.event;

import capitec.branch.appointment.utils.CustomerEmail;
import capitec.branch.appointment.utils.Username;

public record DeleteUserEvent(
        @Username String username,
        @CustomerEmail
        String email,
        String fullname,
        String OTP,
        String traceId
) {
}
