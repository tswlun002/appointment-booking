package capitec.branch.appointment.user.app;

import capitec.branch.appointment.user.domain.UserService;
import capitec.branch.appointment.user.domain.UsernameGenerator;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class GenerateUsernameUseCase {

    private final UserService userService;

    public String execute(String traceId) {
        log.debug("generateUserId traceId:{}", traceId);
        String id;
        do {
            id = new UsernameGenerator().getId();
        } while (userService.checkIfUserExists(id));

        return id;
    }
}
