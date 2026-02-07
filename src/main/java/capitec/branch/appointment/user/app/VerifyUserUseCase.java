package capitec.branch.appointment.user.app;

import capitec.branch.appointment.authentication.domain.AuthUseCase;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.exeption.TokenExpiredException;
import capitec.branch.appointment.otp.app.ValidateOTPService;
import capitec.branch.appointment.role.domain.FetchRoleByNameService;
import capitec.branch.appointment.user.app.event.UserCreatedEvent;
import capitec.branch.appointment.user.app.event.UserVerifiedEvent;
import capitec.branch.appointment.user.domain.*;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class VerifyUserUseCase implements ImpersonateUserLogin {

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final FetchRoleByNameService fetchRoleByNameService;
    private final ValidateOTPService validateOTPService;
    private final AuthUseCase authUseCase;

    @Transactional
    public TokenResponse execute(String username, String otp, boolean isCapitecClient, String traceId) {
        log.info("Verifying user, traceId:{}", traceId);

        var user = userService.getUserByUsername(username).orElseThrow(() -> {
            log.error("User is not found with username {}, traceId:{}", username, traceId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        });

        var validate = false;

        try {
            validate = validateOTPService.validateOTP(user.getUsername(), otp, traceId);

        } catch (TokenExpiredException e) {
            log.error("OTP expired, traceId:{}", traceId, e);
            applicationEventPublisher.publishEvent(new UserCreatedEvent(
                    username,
                    user.getEmail(),
                    user.getFirstname() + " " + user.getLastname(),
                    traceId
            ));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN.value(), e.getMessage() + ",OTP expired, new OTP was sent to your email", e);

        } catch (ResponseStatusException e) {
            log.error("Invalid OTP, traceId:{}", traceId, e);
            if (e.getStatusCode().equals(HttpStatus.LOCKED)) {
                log.error("OTP is locked so disable user, traceId:{}", traceId);
                userService.updateUseStatus(username, false);
            }
            throw e;
        }

        if (!validate) {
            log.error("Failed, OTP is invalid, traceId:{}", traceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Failed, OTP is invalid");
        }

        validate = userService.verifyUser(username);

        if (validate) {
            assignDefaultRoles(username, isCapitecClient, traceId);

            String fullName = user.getFirstname() + " " + user.getLastname();
            applicationEventPublisher.publishEvent(new UserVerifiedEvent(
                    user.getUsername(),
                    user.getEmail(),
                    fullName,
                    otp,
                    traceId
            ));

            TokenResponse tokenResponse;
            try {
                tokenResponse = adminImpersonateUserLogin(user.getUsername(), traceId);
            } catch (Exception e) {
                log.warn("Failed to auto login, user must manually login to complete account setup and access services, traceId:{}", traceId, e);
                return null;
            }

            return tokenResponse;
        } else {
            log.warn("Failed to set user verification status to verified, username:{}, traceId:{}", username, traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Temporary failed to updated verification status, please try again.");
        }
    }

    private void assignDefaultRoles(String username, boolean isCapitecClient, String traceId) {
        String groupName = isCapitecClient ? USER_TYPES.USER_CLIENT.name() : USER_TYPES.USER_GUEST.name();
        Optional<String> groupId = fetchRoleByNameService.getGroupId(groupName, true);

        if (groupId.isPresent()) {
            boolean isAssigned = userRoleService.addUserToGroup(username, groupId.get());

            if (!isAssigned) {
                log.warn("Failed to assign default groups/roles to user. Admin must add these role to user. default groups/roles {} is not assigned to user {},traceId:{}", groupId.get(), username, traceId);
            } else {
                log.info("User assigned default groups/roles:{} to user {}, traceId:{}", groupId.get(), username, traceId);
            }
        } else {
            log.warn("Failed to assign default groups/role to user:{}. Admin must add these default groups/role to user, traceId:{}.", username, traceId);
        }
    }

    @Override
    public TokenResponse adminImpersonateUserLogin(String username, String traceId) {
        return authUseCase.adminImpersonateUser(username, traceId);
    }
}
