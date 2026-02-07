package capitec.branch.appointment.user.infrastructure.adapter;

import capitec.branch.appointment.authentication.domain.AuthUseCase;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.user.app.port.AuthenticationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationAdapter implements AuthenticationPort {

    private final AuthUseCase authUseCase;

    @Override
    public TokenResponse impersonateUser(String username, String traceId) {
        return authUseCase.adminImpersonateUser(username, traceId);
    }
}
