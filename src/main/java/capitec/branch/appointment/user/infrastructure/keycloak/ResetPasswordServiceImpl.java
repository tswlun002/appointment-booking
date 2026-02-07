package capitec.branch.appointment.user.infrastructure.keycloak;

import capitec.branch.appointment.user.domain.ResetPasswordService;
import capitec.branch.appointment.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.stereotype.Service;

import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private final KeycloakUserHelper userHelper;

    @Override
    public boolean passwordReset(@Valid User user) {
        UserResource userResource = userHelper.getUserResource(user.getUsername());

        return keyCloakRequest(() -> {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setValue(user.getPassword());
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setTemporary(Boolean.FALSE);

            userResource.resetPassword(credential);

            log.info("Password reset successful. username: {}", user.getUsername());
            return true;
        }, "reset password", Boolean.class);
    }
}
