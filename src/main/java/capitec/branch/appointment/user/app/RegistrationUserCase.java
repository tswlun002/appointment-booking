package capitec.branch.appointment.user.app;


import capitec.branch.appointment.authentication.domain.AuthUseCase;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.exeption.TokenExpiredException;
import capitec.branch.appointment.otp.app.ValidateOTPService;
import capitec.branch.appointment.role.domain.FetchRoleByNameService;
import capitec.branch.appointment.user.domain.*;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class RegistrationUserCase implements ImpersonateUserLogin{

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final FetchRoleByNameService fetchRoleByNameService;
    private final ValidateOTPService validateOTPService;
    private  final AuthUseCase  authUseCase;
    private final ClientDomain clientDomain;

    public User registerUser(@Valid NewUserDtO registerDTO, String traceId) {

        User user;


        if (registerDTO.isCapitecClient()) {

            user = clientDomain.findByUsername(registerDTO.idNumber()).orElseThrow(() -> {
                log.error("User is not found with idNumber {}, traceId:{}", registerDTO.idNumber(), traceId);
                return new NotFoundException("User not found");
            });
        }
        else {
            do {
                user = new User(registerDTO.email(), registerDTO.firstname(), registerDTO.lastname(), registerDTO.password());
            } while (userService.checkIfUserExists(user.getUsername()));
        }



        userService.getUserByEmail(user.getEmail()).ifPresent(u -> {

            log.error("User already exists with email:{}, traceId:{}", u.getEmail(), traceId);
            throw new EntityAlreadyExistException("User already exists");
        });

        user = userService.registerUser(user);

        if (user == null) {

            log.error("Failed to register new user, traceId:{}", traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to register new user");
        }

        log.info("Registered user, traceId:{}", traceId);
        applicationEventPublisher.publishEvent(new UserCreatedEvent(user.getUsername(), user.getEmail(), user.getFirstname() + " " + user.getLastname(), traceId));

        return user;
    }

    public TokenResponse verifyUser(String username, String otp, boolean isCapitecClient, String traceId) {
        log.info("Verifying user, traceId:{}", traceId);

        var user = userService.getUserByUsername(username).orElseThrow(() -> {

            log.error("User is not found with username {}, traceId:{}", username, traceId);
            return new NotFoundException("User not found");
        });

        var validate = false;

        try {

            validate = validateOTPService.validateOTP(user.getUsername(), otp, traceId);

        } catch (TokenExpiredException e) {
            ///  To BE UPDATED TO UserCreatedEventExpired so that it gives us insight how many OTP expired and
            /// user tries after how long the token expired. And it can help to look into the speed of events and emails are delivered to user
            log.error("OTP expired, traceId:{}", traceId, e);
            applicationEventPublisher.publishEvent(new UserCreatedEvent(username, user.getEmail(), user.getFirstname() + " " + user.getLastname(), traceId));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN.value(), e.getMessage() + ",OTP expired, new OTP was sent to your email", e);

        } catch (ResponseStatusException e) {
            //user tried many times that exceed maximum verify/validate retry, disable user
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
            applicationEventPublisher.publishEvent(new UserVerifiedEvent(user.getUsername(), user.getEmail(), fullName, otp, traceId));

            TokenResponse tokenResponse;
            try {

                tokenResponse = adminImpersonateUserLogin(user.getUsername(), traceId);

            } catch (Exception e) {

                log.warn("Failed to auto login, user must manually login to complete account setup and access services, traceId:{}", traceId, e);

                throw new ResponseStatusException(HttpStatus.ACCEPTED, "OTP verified successfully. Please login manually");
            }

            return tokenResponse;
        } else {

            log.warn("Failed to verify user username, traceId:{}", traceId);
            return null;
        }

    }

    public TokenResponse verifyUser(String username, String otp, String traceId) {
        return verifyUser(username, otp, false, traceId);
    }

    private void assignDefaultRoles(String username, boolean isCapitecClient, String traceId) {

        String groupName = isCapitecClient ? USER_TYPES.USER_CLIENT.name() : USER_TYPES.USER_GUEST.name();
        Optional<String> groupId = fetchRoleByNameService.getGroupId(groupName, true);

        if (groupId.isPresent()) {

            boolean isAssigned = userRoleService.addUserToGroup(username, groupId.get());

            if (!isAssigned) {

                log.warn("Failed to assign default groups/roles to user. Admin must add these role to user. default groups/roles {} is not assigned to user {},traceId:{}", groupId.get(), username, traceId);
            }
            else {

                log.info("User assigned default groups/roles:{} to user {}, traceId:{}", groupId.get(), username, traceId);
            }
        }
        else {
            log.warn("Failed to assign default groups/role to user:{}. Admin must add these default groups/role to user, traceId:{}.", username, traceId);
        }
    }

    public User getUser(String username) {
        return userService.getUserByUsername(username).orElseThrow(() -> {
            log.error("User not found with username:{}", username);
            return new NotFoundException("User is not found");
        });
    }

    public User getUserByEmail(String email) {
        return userService.getUserByEmail(email).orElseThrow(
                () -> {
                    log.error("User is not found with email:{}", email);
                    return new NotFoundException("User not found");
                }
        );
    }

    @Override
    public TokenResponse adminImpersonateUserLogin(String username, String traceId) {
        return authUseCase.adminImpersonateUser(username, traceId);
    }
}
