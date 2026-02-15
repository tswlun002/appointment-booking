package capitec.branch.appointment.user.app;

import capitec.branch.appointment.user.app.port.UserQueryPort;
import capitec.branch.appointment.sharekernel.username.UsernameGenerator;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class GenerateUsernameUseCase {

    private final UserQueryPort userQueryPort;

    public String execute(String traceId) {
        log.debug("generateUserId traceId:{}", traceId);
        String id;
        do {
            id = new UsernameGenerator().getId();
        } while (userQueryPort.checkIfUserExists(id));

        return id;
    }
}
