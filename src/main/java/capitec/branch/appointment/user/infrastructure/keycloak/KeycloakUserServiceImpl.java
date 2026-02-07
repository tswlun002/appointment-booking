package capitec.branch.appointment.user.infrastructure.keycloak;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.user.domain.UserDomainException;
import capitec.branch.appointment.user.domain.UserService;
import capitec.branch.appointment.user.infrastructure.UserMapperReflection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.core.Response;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;

@Slf4j
@Service
public class KeycloakUserServiceImpl implements UserService {

    private final String authType;
    private final KeycloakService keycloakService;
    private final KeycloakUserHelper userHelper;

    public KeycloakUserServiceImpl(
            @Value("${keycloak.user_auth_type}") String authType,
            KeycloakService keycloakService,
            KeycloakUserHelper userHelper) {
        this.authType = authType;
        this.keycloakService = keycloakService;
        this.userHelper = userHelper;
    }

    @Override
    public User registerUser(@Valid final User user) {
        var credentialRepresentation = createCredential(user);
        var keyCloakUser = createUserRepresentation(user, user.isEnabled(), user.isVerified(), credentialRepresentation);

        Response response = keyCloakRequest(
                () -> keycloakService.getUsersResources().create(keyCloakUser),
                "register user",
                Response.class
        );

        handleRegistrationResponse(response, user);
        return user;
    }

    @Override
    public boolean verifyUser(String username) {
        boolean result = keyCloakRequest(() -> {
            UserRepresentation representation = userHelper.findByUsernameOrThrow(username);
            representation.setEmailVerified(true);
            representation.setRequiredActions(List.of());

            UserResource userResource = userHelper.getUsersResource().get(representation.getId());
            userResource.update(representation);

            log.debug("User verified. username: {}", username);
            return true;
        }, "verify user", Boolean.class);

        userHelper.evictUserCache(username);
        return result;
    }

    @Override
    public boolean verifyUserCurrentPassword(String username, String password, String traceId) {
        UserRepresentation userRep = userHelper.findByUsernameOrThrow(username);
        return keycloakService.verifyUserPassword(userRep.getUsername(), password, traceId);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userHelper.findByUsername(username)
                .map(UserMapperReflection::mapToUser);
    }

    @Override
    public Optional<User> getUserByEmail(@NotBlank @Email String email) {
        return userHelper.findByEmail(email)
                .map(UserMapperReflection::mapToUser);
    }

    @Override
    public boolean deleteUser(String username) {
        UserRepresentation userRep = userHelper.findByUsernameOrThrow(username);

        Response response = keyCloakRequest(
                () -> keycloakService.getUsersResources().delete(userRep.getId()),
                "delete user",
                Response.class
        );

        boolean deleted = handleDeleteResponse(response, username);
        if (deleted) {
            userHelper.evictUserCache(username);
        }
        return deleted;
    }

    @Override
    public boolean checkIfUserExists(String username) {
        return userHelper.findByUsername(username).isPresent();
    }

    @Override
    public void updateUseStatus(String username, Boolean useStatus) {
        keyCloakRequest(() -> {
            UserRepresentation representation = userHelper.findByUsernameOrThrow(username);
            representation.setEnabled(useStatus);

            UserResource userResource = userHelper.getUsersResource().get(representation.getId());
            userResource.update(representation);

            log.info("User status updated. username: {}, enabled: {}", username, useStatus);
            return true;
        }, "update user status", Boolean.class);

        userHelper.evictUserCache(username);
    }

    @Override
    public boolean resetPassword(@Valid User user) {
        Assert.notNull(user, "User must not be null");
        Assert.hasText(user.getPassword(), "User password must not be blank");

        UserResource userResource = userHelper.getUserResource(user.getUsername());

        boolean result = keyCloakRequest(() -> {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setValue(user.getPassword());
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setTemporary(Boolean.FALSE);

            userResource.resetPassword(credential);

            log.info("Password reset successful. username: {}", user.getUsername());
            return true;
        }, "reset password", Boolean.class);

        if (result) {
            userHelper.evictUserCache(user.getUsername());
        }

        return result;
    }

    // ==================== Private Helper Methods ====================

    private void handleRegistrationResponse(Response response, User user) {
        HttpStatus status = HttpStatus.valueOf(response.getStatus());

        if (status.is2xxSuccessful()) {
            log.info("User registered successfully. username: {}", user.getUsername());
            return;
        }

        if (status == HttpStatus.CONFLICT) {
            log.info("User already exists. username: {}", user.getUsername());
            throw new EntityAlreadyExistException("User already exists");
        }

        if (status.is5xxServerError()) {
            log.error("Failed to register user. username: {}, status: {}, message: {}",
                    user.getUsername(), response.getStatus(), response.getStatusInfo());
            throw new UserDomainException("Failed to register user. Service temporarily unavailable.");
        }

        log.error("Unexpected response during registration. username: {}, status: {}",
                user.getUsername(), response.getStatus());
        throw new UserDomainException("Failed to register user");
    }

    private boolean handleDeleteResponse(Response response, String username) {
        HttpStatusCode statusCode = HttpStatusCode.valueOf(response.getStatus());

        if (statusCode.is2xxSuccessful()) {
            log.info("User deleted successfully. username: {}", username);
            return true;
        }

        if (statusCode.is5xxServerError()) {
            log.error("Keycloak service unavailable during delete. username: {}", username);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Service temporarily unavailable. Please try again later.");
        }

        log.error("Failed to delete user. username: {}, status: {}", username, response.getStatus());
        throw new ResponseStatusException(HttpStatus.valueOf(response.getStatus()),
                response.getStatusInfo().getReasonPhrase());
    }

    @NonNull
    private CredentialRepresentation createCredential(User user) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(AuthType.fromAuthType(authType).name());
        credential.setTemporary(Boolean.FALSE);
        credential.setValue(user.getPassword());
        return credential;
    }


    @NonNull
    private static UserRepresentation createUserRepresentation(
            User user,
            boolean isEnabled,
            boolean isVerified,
            CredentialRepresentation credential) {

        var keyCloakUser = new UserRepresentation();
        keyCloakUser.setUsername(user.getUsername());
        keyCloakUser.setEmail(user.getEmail());
        keyCloakUser.setFirstName(user.getFirstname());
        keyCloakUser.setLastName(user.getLastname());
        keyCloakUser.setEnabled(isEnabled);
        keyCloakUser.setEmailVerified(isVerified);
        keyCloakUser.setCredentials(List.of(credential));
        keyCloakUser.setRequiredActions(List.of("VERIFY_EMAIL"));
        keyCloakUser.setCreatedTimestamp(LocalDateTime.now().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());

        return keyCloakUser;
    }
}
