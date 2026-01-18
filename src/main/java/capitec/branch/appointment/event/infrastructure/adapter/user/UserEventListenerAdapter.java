package capitec.branch.appointment.event.infrastructure.adapter.user;

import capitec.branch.appointment.event.app.port.OTPEventProducerServicePort;
import capitec.branch.appointment.event.app.port.OTPPort;
import capitec.branch.appointment.event.app.port.UserEventListenerPort;
import capitec.branch.appointment.user.app.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListenerAdapter {

    private final UserEventListenerPort userEventListenerPort;
    private  final OTPEventProducerServicePort otpCreationService;

    private final OTPPort otpPort;

    @EventListener(UserVerifiedEvent.class)
    public void onUserVerified(@Valid UserVerifiedEvent event) {
        log.info("Received user verified event, traceId: {}", event.traceId());

        userEventListenerPort.handleUserVerifiedEvent(event.username(), event.email(),event.fullName(),event.otp(), event.traceId());
    }

    @EventListener(DeleteUserEvent.class)
    public void onDeleteUser(@Valid DeleteUserEvent event) {
        log.info("Received delete user event, traceId: {}", event.traceId());
        otpPort.deleteOTP(event.username(), event.traceId());
        userEventListenerPort.handleDeleteUserEvent(event.username(), event.email(),event.fullname(),event.OTP(), event.traceId());
    }

    @EventListener(PasswordUpdatedEvent.class)
    public void onPasswordUpdated(@Valid PasswordUpdatedEvent event) {
        log.info("Received password updated event, traceId: {}", event.traceId());
        userEventListenerPort.handlePasswordUpdatedEvent(event.username(), event.email(),event.fullname(),event.otp(), event.traceId());
    }
    @EventListener(UserCreatedEvent.class)
    public void onUserCreated(@Valid UserCreatedEvent event) {
        log.info("Received user registration event, traceId: {}", event.traceId());
        var otp = otpPort.generateOTP(event.username(), event.email(),"EMAIL_VERIFICATION");
        otpCreationService.sendRegistrationEvent(event.username(), event.email(), event.fullname(), otp, event.traceId());

    }

    @EventListener(PasswordResetRequestEvent.class)
    public void onPasswordResetRequest(@Valid PasswordResetRequestEvent event) {
        log.info("Received password reset request event, traceId: {}", event.traceId());
        var otp = otpPort.generateOTP(event.username(), event.email(), "PASSWORD_RESET");
        otpCreationService.sendPasswordResetRequestEvent(event.username(), event.email(), event.fullname(), otp, event.traceId());
    }

    @EventListener(DeleteUserRequestEvent.class)
    public void onDeleteUserRequest(@Valid DeleteUserRequestEvent event) {
        log.info("Received delete user request event, traceId: {}", event.traceId());
        var otp = otpPort.generateOTP(event.username(), event.email(), "ACCOUNT_DELETION");
        otpCreationService.deleteUserRequestEvent(event.username(), event.email(), event.fullname(), otp, event.traceId());
    }

}
