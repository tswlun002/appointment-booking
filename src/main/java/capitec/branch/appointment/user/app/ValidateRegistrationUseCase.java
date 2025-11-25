package capitec.branch.appointment.user.app;

import capitec.branch.appointment.user.domain.UserId;
import capitec.branch.appointment.user.domain.UserService;
import capitec.branch.appointment.utils.UseCase;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class ValidateRegistrationUseCase {
    private final UserService userService;

    public  boolean execute(UserId userId) {
      return false;
    }
}
