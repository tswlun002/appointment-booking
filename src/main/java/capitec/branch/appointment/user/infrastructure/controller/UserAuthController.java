package capitec.branch.appointment.user.infrastructure.controller;

import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.user.app.*;
import capitec.branch.appointment.user.app.dto.EmailCommand;
import capitec.branch.appointment.user.app.dto.NewUserDtO;
import capitec.branch.appointment.user.app.dto.PasswordResetDTO;

import capitec.branch.appointment.utils.ValidatorMessages;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.core.NewCookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@Slf4j
@RestController
@RequestMapping(value = "/api/v1/users/auth")
@RequiredArgsConstructor
@Validated
public class UserAuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyUserUseCase verifyUserUseCase;
    private final GetUserQuery getUserQuery;
    private final GenerateUsernameUseCase generateUsernameUseCase;
    private final PasswordResetUseCase passwordResetUseCase;

    @Value("${cookie.samesite}")
    private NewCookie.SameSite samesiteCookie;


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody @Valid NewUserDtO newUserDtO, @RequestHeader("Trace-Id") String traceId) {

        log.info("Registering user traceId:{}", traceId);

        registerUserUseCase.execute(newUserDtO, traceId);

        return new ResponseEntity<>("Verification code, please confirm it to complete registration", HttpStatus.CREATED);
    }

    @PutMapping("/verify")
    public void verifyUser(@RequestBody @Valid VerificationDTO verification, @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {

        log.info("Verification user, traceId:{}", traceId);

        var user = getUserQuery.execute(new EmailCommand(verification.email()));

        var tokenResponseOptional = verifyUserUseCase.execute(user.getUsername(), verification.otp(), verification.isCapitecClient(), traceId);

        if (tokenResponseOptional.isEmpty()) {

            log.info("Auto login user failed, traceId:{}", traceId);
            response.setStatus(HttpStatus.ACCEPTED.value());
            try {
                new ObjectMapper().writeValue(response.getOutputStream(), "OTP verified successfully. Please can login");
            } catch (IOException e) {
                log.warn("Failed to set response body, traceId:{}", traceId, e);
                response.setStatus(HttpStatus.NO_CONTENT.value());
            }
        } else {
            addTokenToCookie(tokenResponseOptional.get(), response, traceId);
        }
    }

    private void addTokenToCookie(TokenResponse token, HttpServletResponse response, String traceId) {

        Cookie cookie = new Cookie("refresh_token", token.getRefreshToken());
        cookie.setMaxAge((int) (token.getRefreshExpiresIn()));
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setAttribute("scope", token.getScope());
        cookie.setAttribute("samesite", samesiteCookie.name());
        cookie.setPath("/");
        response.addCookie(cookie);
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        try {

            token.setRefreshToken(null);
            token.setRefreshExpiresIn(0);
            new ObjectMapper().writeValue(response.getOutputStream(), token);

        } catch (Exception e) {
            log.warn("Failed to write token to response, traceId:{}", traceId, e);
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }


    @GetMapping("/credentials/password/request-reset")
    public ResponseEntity<?> requestToResetPassword(@RequestParam("email") @Email(message = ValidatorMessages.EMAIL_MESS) @NotBlank(message = ValidatorMessages.EMAIL_MESS) String email,
                                                    @RequestHeader("Trace-Id") String traceId) {
        log.info("Reseting password for user: {} traceId:{}", email, traceId);
        passwordResetUseCase.passwordResetRequest(email, traceId);
        return new ResponseEntity<>("Email verification sent to your email", HttpStatus.OK);
    }

    @PutMapping("/credentials/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid PasswordResetDTO passwordResetDTO, @RequestHeader("Trace-Id") String traceId) {
        log.info("Updating password for user: {} traceId:{}", passwordResetDTO.email(), traceId);
        passwordResetUseCase.passwordReset(passwordResetDTO, traceId);
        return new ResponseEntity<>("Password updated successfully", HttpStatus.OK);
    }

    @GetMapping("/generate/username")
    public ResponseEntity<?> generateNewUserId(@RequestHeader("Trace-Id") String traceId) {
        log.info("Generating user username:{}", traceId);
        return new ResponseEntity<>(generateUsernameUseCase.execute(traceId), HttpStatus.OK);
    }
}
