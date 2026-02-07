package capitec.branch.appointment.user.app;

import capitec.branch.appointment.exeption.TokenExpiredException;
import capitec.branch.appointment.otp.app.ValidateOTPService;
import capitec.branch.appointment.user.app.event.DeleteUserEvent;
import capitec.branch.appointment.user.app.event.DeleteUserRequestEvent;
import capitec.branch.appointment.user.domain.UserService;
import capitec.branch.appointment.utils.UseCase;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class DeleteUserUseCase {
    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ValidateOTPService validateOTPService;

    public boolean deleteUserRequest(String username, String password, String traceId) {

        var user = userService.getUserByUsername(username).orElseThrow(() -> {
            log.error("User not found with username:{}", username);
            return new NotFoundException("User not found.");
        });

        boolean isVerified = userService.verifyUserCurrentPassword(username, password, traceId);

        if (isVerified) {

            var fullName = user.getFirstname() + " " + user.getLastname();
            applicationEventPublisher.publishEvent(new DeleteUserRequestEvent(user.getUsername(), user.getEmail(), fullName, traceId));
        }

        return isVerified;

    }

    public boolean deleteUser(String username, String OTP, String traceId) {

        var user = userService.getUserByUsername(username).orElseThrow(() -> {

            log.error("User not found, traceId:{}", traceId);
            return new NotFoundException("User not found.");
        });

        boolean isVerified;
        try {

            isVerified = validateOTPService.validateOTP(username, OTP, traceId);

        } catch (TokenExpiredException e) {
            ///  To BE UPDATED TO UserDeletedEventExpired so that it gives us insight how many OTP expired and
            /// user tries after how long the token expired. And it can help to look into the speed of events and emails are delivered to user
            log.error("OTP expired, traceId:{}", traceId,e);
            applicationEventPublisher.publishEvent(new DeleteUserRequestEvent(username, user.getEmail(), user.getFirstname() + " " + user.getLastname(), traceId));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN.value(), e.getMessage() + ",OTP expired, new OTP was sent to your email", e);

        } catch (ResponseStatusException e) {
            //user tried many times that exceed maximum verify/validate retry, disable user
            log.error("Invalid OTP, traceId:{}", traceId,e);
            if (e.getStatusCode().equals(HttpStatus.LOCKED)) {

                log.error("OTP is locked so disable user, traceId:{}", traceId);
                userService.updateUseStatus(username, false);
            }
            throw e;

        }

        if(!isVerified) {

            log.error("Failed, OTP is invalid, traceId:{}", traceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Failed, OTP is invalid");
        }

        isVerified =   userService.deleteUser(username);

        if (isVerified) {

            var fullName = user.getFirstname() + " " + user.getLastname();
            applicationEventPublisher.publishEvent(new DeleteUserEvent(user.getUsername(), user.getEmail(), fullName, OTP, traceId));
        }


        return isVerified;
    }


}
