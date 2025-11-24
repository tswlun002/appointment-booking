package lunga.appointmentbooking.user.app;

import lunga.appointmentbooking.user.domain.UserId;
import lunga.appointmentbooking.user.domain.UserService;
import lunga.appointmentbooking.utils.UseCase;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class ValidateRegistrationUseCase {
    private final UserService userService;

    public  boolean execute(UserId userId) {
      return false;
    }
}
