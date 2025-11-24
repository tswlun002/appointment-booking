package lunga.appointmentbooking.user.app;
import lunga.appointmentbooking.exeption.TokenExpiredException;
import lunga.appointmentbooking.otp.app.ValidateOTPService;
import lunga.appointmentbooking.user.domain.ResetPasswordService;
import lunga.appointmentbooking.user.domain.User;
import lunga.appointmentbooking.user.domain.UserService;
import lunga.appointmentbooking.utils.UseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class PasswordResetUseCase {

    private final UserService userService;
    private final ResetPasswordService resetPasswordService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ValidateOTPService validateOTPService;

    public void passwordResetRequest(@Email @NotBlank String email, String traceId) {

        Optional<User> user = userService.getUserByEmail(email);

        if (user.isPresent()) {

            log.info("Password reset requested event issued, traceId: {}", traceId);
            applicationEventPublisher.publishEvent(new PasswordResetRequestEvent(user.get().getUsername(), user.get().getEmail(), user.get().getFirstname() + " " + user.get().getLastname(), traceId));
        } else {
            log.error("User is not found for email ,traceId{}", traceId);
            throw new NotFoundException("User is not found");
        }

    }

    public boolean passwordChangeRequest(String username, String password, String traceId) {

        var user = userService.getUserByUsername(username).orElseThrow(() -> {
            log.warn("User not found, traceId:{}", traceId);
            throw new NotFoundException("User is not found");
        });

        boolean isVerified = userService.verifyUserCurrentPassword(username, password);

        if (isVerified) {
            log.info("Password change requested event issued, traceId: {}", traceId);
            applicationEventPublisher.publishEvent(new PasswordResetRequestEvent(username, user.getEmail(), user.getFirstname() + " " + user.getLastname(), traceId));
        }

        return isVerified;

    }

    public void passwordReset(@Valid PasswordResetDTO passwordResetDTO, String traceId) {

        User user = userService.getUserByEmail(passwordResetDTO.email()).orElseThrow(() -> {
            log.warn("User not found for email, traceId:{}", traceId);
            return new NotFoundException("User is not found");
        });

        boolean validated;

        try {
            validated = validateOTPService.validateOTP(user.getUsername(), passwordResetDTO.OTP(), traceId);
        } catch (TokenExpiredException e) {
            ///  To BE UPDATED TO UserPasswordResetEventExpired so that it gives us insight how many OTP expired and
            /// user tries after how long the token expired. And it can help to look into the speed of events and emails are delivered to user
            log.error("OTP expired, traceId:{}", traceId,e);
            applicationEventPublisher.publishEvent(new PasswordResetRequestEvent(user.getUsername(), user.getEmail(), user.getFirstname() + " " + user.getLastname(), traceId));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN.value(), e.getMessage() + ",OTP expired, new OTP was sent to your email", e);

        } catch (ResponseStatusException e) {
            //user tried many times that exceed maximum verify/validate retry, disable user
            log.error("Invalid OTP, traceId:{}", traceId,e);
            if (e.getStatusCode().equals(HttpStatus.LOCKED)) {

                log.error("OTP is locked so disable user, traceId:{}", traceId);
                userService.updateUseStatus(user.getUsername(), false);
            }
            throw e;

        }

        if(!validated) {

            log.error("Failed to reset password. Invalid OTP, traceId:{}", traceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Failed to reset user password, invalid OTP");

        }

        user.setPassword(passwordResetDTO.newPassword());

        validated =  resetPasswordService.passwordReset(user);

        if (validated) {

            log.info("Password reset successfully and Password Updated Event event issued, traceId: {}", traceId);
            applicationEventPublisher.publishEvent(new PasswordUpdatedEvent(user.getUsername(), user.getEmail(),user.getFirstname() + " " + user.getLastname(),  passwordResetDTO.OTP(), traceId));
        }
        else {
            log.error("Password reset  failed, traceId {}",  traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to reset user password");
        }

    }

    public void passwordChange(@Valid ChangePasswordDTO changePasswordDTO, String traceId) {

        User user = userService.getUserByUsername(changePasswordDTO.username()).orElseThrow(() -> {
            log.error("User not found, traceId:{}",  traceId);
            return new NotFoundException("User is not found");
        });

        var  validated=false;

        try {
            validated = validateOTPService.validateOTP(user.getUsername(), changePasswordDTO.OTP(), traceId);
        } catch (TokenExpiredException e) {
            ///  To BE UPDATED TO UserPasswordResetEventExpired so that it gives us insight how many OTP expired and
            /// user tries after how long the token expired. And it can help to look into the speed of events and emails are delivered to user
            log.error("OTP expired, traceId:{}", traceId,e);
            applicationEventPublisher.publishEvent(new PasswordResetRequestEvent(user.getUsername(), user.getEmail(), user.getFirstname() + " " + user.getLastname(), traceId));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN.value(), e.getMessage() + ",OTP expired, new OTP was sent to your email", e);

        } catch (ResponseStatusException e) {
            //user tried many times that exceed maximum verify/validate retry, disable user
            log.error("Invalid OTP, traceId:{}", traceId,e);
            if (e.getStatusCode().equals(HttpStatus.LOCKED)) {

                log.error("OTP is locked so disable user, traceId:{}", traceId);
                userService.updateUseStatus(user.getUsername(), false);
            }
            throw e;

        }

        if (!validated) {
            log.error("Password change  failed, traceId {}",  traceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Failed to reset user password, invalid OTP");
        }

        user.setPassword(changePasswordDTO.newPassword());
        validated = resetPasswordService.passwordReset(user);
        if (validated) {

            PasswordUpdatedEvent event = new PasswordUpdatedEvent(user.getUsername(),  user.getEmail(),user.getFirstname() + " " + user.getLastname(), changePasswordDTO.OTP(), traceId);

            log.info("Password change successfully and Password Updated Event issued, traceId {}", traceId);
            applicationEventPublisher.publishEvent(event);
        }
        else{
            log.error("Password change failed, traceId {}",  traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to change user password");
        }
    }
}
