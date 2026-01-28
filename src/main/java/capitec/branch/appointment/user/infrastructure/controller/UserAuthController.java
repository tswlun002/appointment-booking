package capitec.branch.appointment.user.infrastructure.controller;

import capitec.branch.appointment.user.app.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.user.app.*;
import capitec.branch.appointment.utils.Validator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@Slf4j
@RestController
@RequestMapping(value = "/api/v1/users/auth")
@RequiredArgsConstructor
@Validated
public class UserAuthController {

    private  final RegistrationUserCase registrationUserCase;
    private final PasswordResetUseCase passwordResetUseCase;


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody @Valid NewUserDtO newUserDtO, @RequestHeader("Trace-Id") String traceId) {

        log.info("Registering user traceId:{}", traceId);

      registrationUserCase.registerUser(newUserDtO,traceId);

        return new ResponseEntity<>("Verification code, please confirm it to complete registration", HttpStatus.CREATED);


    }
    @PutMapping("/verify")
    public  void verifyUser(@RequestBody @Valid VerificationDTO verification , @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {

        log.info("Verification user, traceId:{}",traceId);

        var user = registrationUserCase.getUserByEmail(verification.email());

        var tokenResponse=registrationUserCase.verifyUser(user.getUsername(), verification.otp(), traceId);

        if(tokenResponse==null){

            log.error("Failed to verify user, traceId:{}", traceId);
            throw  new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to verify user, OTP incorrect");
        }

        addTokenToCookie(tokenResponse, response, traceId);
    }

    private  void addTokenToCookie(TokenResponse token, HttpServletResponse response, String traceId) {

        //Added refresh token to cookies and secure it
        Cookie cookie = new Cookie("refresh_token", token.getRefreshToken());
        cookie.setMaxAge((int)(token.getRefreshExpiresIn())); //set in seconds
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setAttribute("scope", token.getScope());
        cookie.setPath("/"); // global cookie accessible everywhere
        response.addCookie(cookie);

        //Add access token response body
        //Map<String, TokenResponse> tokens = new HashMap<>();
       // tokens.put("access_token", token);

        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        try {

            token.setRefreshToken(null);
            token.setRefreshExpiresIn(0);
            new ObjectMapper().writeValue(response.getOutputStream(), token);

        } catch (IOException e) {
            log.error("Failed to write token to response, traceId:{}", traceId,e);
            throw new InternalServerErrorException("Internal Server Error");
        }
    }

    @GetMapping("/credentials/password/request-reset")
    public ResponseEntity<?> requestToResetPassword(@RequestParam("email")@Email(message= Validator.EMAIL_MESS) @NotBlank(message = Validator.EMAIL_MESS) String email,
                                           @RequestHeader("Trace-Id") String traceId) {
        log.info("Reseting password for user: {} traceId:{}", email, traceId);
        passwordResetUseCase.passwordResetRequest(email, traceId);
        return new ResponseEntity<>("Email verification sent to your email", HttpStatus.OK);
    }
    @PutMapping("/credentials/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid PasswordResetDTO passwordResetDTO, @RequestHeader("Trace-Id") String traceId) {
        log.info("Updating password for user: {} traceId:{}", passwordResetDTO.email(), traceId);
        passwordResetUseCase.passwordReset(passwordResetDTO, traceId);
        return new ResponseEntity<>("Password updated  successfully", HttpStatus.OK);
    }

    @GetMapping("/generate/username")
    public ResponseEntity<?> generateNewUserId(@RequestHeader("Trace-Id") String traceId) {
        log.info("Generating user username:{}", traceId);
        return new ResponseEntity<>(registrationUserCase.generateUserId(traceId), HttpStatus.OK);
    }

}
