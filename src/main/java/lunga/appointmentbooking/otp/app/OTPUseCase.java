package lunga.appointmentbooking.otp.app;



import lunga.appointmentbooking.*;
import lunga.appointmentbooking.exeption.TokenExpiredException;
import lunga.appointmentbooking.user.app.DeleteUserEvent;
import lunga.appointmentbooking.user.app.DeleteUserRequestEvent;
import lunga.appointmentbooking.user.app.PasswordResetRequestEvent;
import lunga.appointmentbooking.user.app.UserCreatedEvent;
import lunga.appointmentbooking.utils.UseCase;
import lunga.appointmentbooking.otp.domain.*;
import jakarta.validation.Valid;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunga.appointmentbooking.utils.Username;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class OTPUseCase implements ValidateOTPService {

    private final OTPService otpService;
    private final OTPEventProducerService kafkaEventProducerService;
    private final ApplicationEventPublisher eventPublisher;
    @Value("${otp.number.verification.attempts:2}")
    public  int MAX_NUMBER_OF_VERIFICATION_ATTEMPTS;
    @Value("${otp.expire.datetime}")
    private long EXPIRE_DATETIME;

    @EventListener(UserCreatedEvent.class)
    public void create(@Valid UserCreatedEvent userCreatedEvent) {

        log.info("Received user registration event, traceId: {}", userCreatedEvent.traceId());
        var email = userCreatedEvent.email();
        var traceId = userCreatedEvent.traceId();
        var verificationAttempts = new VerificationAttempts(0, MAX_NUMBER_OF_VERIFICATION_ATTEMPTS);
        var savedOtp = otpService.saveOTP(new OTP(userCreatedEvent.username(), EXPIRE_DATETIME,OTP_PURPOSE_ENUM.EMAIL_VERIFICATION, verificationAttempts));


        if (savedOtp != null) {
            kafkaEventProducerService.sendRegistrationEvent(
                    userCreatedEvent.username(), email, userCreatedEvent.fullname(),
                    savedOtp.getCode(), traceId
            ).whenComplete((event, ex) -> {

                if (event!=null && event) {
                    log.info("User registration event was successfully published, traceId: {}", userCreatedEvent.traceId());
                } else {

                    log.error("Failed to publish user registration event, traceId;{}", traceId,ex);
                    eventPublisher.publishEvent(new FailedCreateOTPEvent(userCreatedEvent.username(), traceId));
                    throw new RuntimeException("Failed to publish user registration event",ex);
                }
            });
        }

    }

    @EventListener(PasswordResetRequestEvent.class)
    public void passwordResetRequest(@Valid PasswordResetRequestEvent passwordResetRequestEvent) {

        log.info("Received user password reset request  event, traceId: {}", passwordResetRequestEvent.traceId());
        var email = passwordResetRequestEvent.email();
        var traceId = passwordResetRequestEvent.traceId();
        var verificationAttempts = new VerificationAttempts(0, MAX_NUMBER_OF_VERIFICATION_ATTEMPTS);
        var savedOtp = otpService.saveOTP(new OTP(passwordResetRequestEvent.username(), EXPIRE_DATETIME,OTP_PURPOSE_ENUM.PASSWORD_RESET, verificationAttempts));

        if (savedOtp != null) {
             kafkaEventProducerService.sendPasswordResetRequestEvent(passwordResetRequestEvent.username(), email, passwordResetRequestEvent.fullname(), savedOtp.getCode(), traceId)
             .whenComplete((event, ex) -> {

                 if (event) {
                     log.info("Sent password reset request event, traceId: {}", passwordResetRequestEvent.traceId());
                 } else {

                     log.error("Failed to password reset request event, traceId;{}", traceId,ex);
                     eventPublisher.publishEvent(new FailedCreateOTPEvent(passwordResetRequestEvent.username(), traceId));
                     throw new RuntimeException("Failed to send user verification event",ex);
                 }
             });
        }

    }

    @EventListener(DeleteUserRequestEvent.class)
    public void deleteUserRequest(@Valid DeleteUserRequestEvent userRequestEvent) {

        log.info("Received delete user request event, traceId:{}", userRequestEvent.traceId());
        var email = userRequestEvent.email();
        var traceId = userRequestEvent.traceId();
        var verificationAttempts = new VerificationAttempts(0, MAX_NUMBER_OF_VERIFICATION_ATTEMPTS);
        var savedOtp = otpService.saveOTP(new OTP(userRequestEvent.username(), EXPIRE_DATETIME,OTP_PURPOSE_ENUM.ACCOUNT_DELETION, verificationAttempts));

        if (savedOtp != null) {
            kafkaEventProducerService.deleteUserRequestEvent(userRequestEvent.username(), email, userRequestEvent.fullname(), savedOtp.getCode(), traceId)
                    .whenComplete( (event, ex) -> {

                        if (event) {

                            log.info("Published delete user request event, traceId:{}", userRequestEvent.traceId());
                        }
                        else {

                            log.error("Failed to publish delete user request  event, traceId;{}", traceId,ex);
                            eventPublisher.publishEvent(new FailedCreateOTPEvent(userRequestEvent.username(), traceId));
                            throw new RuntimeException("Failed to publish user verification event",ex);
                        }
                    });
        }

    }


    @Override
    public boolean validateOTP(@Username String username, String otp, String traceId) {

        Optional<OTP> otpEntity;
        try {

            otpEntity = otpService.validateOTP(username, otp, MAX_NUMBER_OF_VERIFICATION_ATTEMPTS);

        } catch (Exception e) {

            log.error("Failed to validate otp, traceId:{}",traceId, e);
            throw new InternalServerErrorException("Failed to validate otp");
        }


        if (otpEntity.isEmpty()) {
            return false;
        }

        var statusEnum = OTPSTATUSENUM.valueOf(otpEntity.get().getStatus().status());
        return switch (statusEnum) {

            case OTPSTATUSENUM.VALIDATED -> true;

            case OTPSTATUSENUM.REVOKED -> {

                log.error("Too many attempt to verify OTP, traceId:{}", traceId);
                throw new ResponseStatusException(HttpStatus.LOCKED, "Too many attempt to verify OTP, please try 5 hours later.");
            }
            case OTPSTATUSENUM.EXPIRED -> {

                log.error("OTP expired , traceId:{}", traceId);

                otpService.renewOTP(otpEntity.get());

                throw new TokenExpiredException("OTP is expired");
            }
            case OTPSTATUSENUM.VERIFIED -> {

                log.error("OTP already verified , traceId:{}", traceId);
                throw new ResponseStatusException(HttpStatus.IM_USED, "OTP is already verified");
            }
            default -> false;
        };

    }

    @EventListener(DeleteUserEvent.class)
    public void onDeletedUserOTPsEvent(DeleteUserEvent event) {

        log.info("Received delete user OTP event, traceId:{}", event.traceId());

        var username = event.username();
        String traceId = event.traceId();

        var allDeleted = otpService.deleteUserOTP(username);

        if (allDeleted) {

            log.info("All user OTP are deleted, traceId:{}", traceId);
        } else {

            log.error("Failed to delete OTP, traceId:{}", traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to delete user.");
        }

    }
}
