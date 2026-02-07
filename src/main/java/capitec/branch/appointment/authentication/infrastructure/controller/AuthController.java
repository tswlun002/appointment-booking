package capitec.branch.appointment.authentication.infrastructure.controller;

import capitec.branch.appointment.authentication.app.*;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.NewCookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST Controller for authentication operations.
 * Provides endpoints for login, logout, token refresh, and password verification.
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final VerifyPasswordUseCase verifyPasswordUseCase;
    private final ImpersonateUserUseCase impersonateUserUseCase;

    @Value("${cookie.samesite}")
    private NewCookie.SameSite sameSite;

    /**
     * User login.
     */
    @PostMapping("/login")
    public void login(@RequestBody LoginDTO loginDTO, @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {
        log.info("User signing in. traceId: {}", traceId);
        TokenResponse token = loginUseCase.execute(loginDTO, traceId);
        addTokenToCookie(token, response, traceId);
    }

    /**
     * Refresh access token using refresh token from cookie.
     */
    @PostMapping("/refresh")
    public void refresh(@RequestHeader(HttpHeaders.COOKIE) String cookies, @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(cookies);

        log.info("Refreshing user token. traceId: {}", traceId);
        TokenResponse tokenResponse = refreshTokenUseCase.execute(refreshToken, traceId);
        addTokenToCookie(tokenResponse, response, traceId);
    }

    /**
     * Verify user's current password.
     */
    @PostMapping("/credentials/password/verify")
    public ResponseEntity<String> verifyUserCurrentPassword(
            @RequestBody UserCredentialDTO userCredentialDTO,
            @RequestHeader("Trace-Id") String traceId) {
        boolean isValid = verifyPasswordUseCase.execute(
                userCredentialDTO.username(),
                userCredentialDTO.Password(),
                traceId
        );
        return new ResponseEntity<>(
                isValid ? "Password is valid." : "Invalid credentials",
                isValid ? HttpStatus.OK : HttpStatus.UNAUTHORIZED
        );
    }

    /**
     * Admin impersonate user login.
     */
    @PostMapping("/admin/impersonate/{username}")
    @PreAuthorize("hasAnyRole('impersonate') and hasAnyRole('admin')")
    public ResponseEntity<TokenResponse> adminImpersonateUser(
            @PathVariable("username") String username,
            @RequestHeader("Trace-Id") String traceId) {
        TokenResponse tokenResponse = impersonateUserUseCase.execute(username, traceId);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * User logout.
     */
    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('app_user')")
    public void logout(@RequestHeader(HttpHeaders.COOKIE) String cookies, @RequestHeader("Trace-Id") String traceId) {
        String refreshToken = extractRefreshToken(cookies);

        log.info("User signing out. traceId: {}", traceId);
        logoutUseCase.execute(refreshToken, traceId);
    }

    // ==================== Private Helper Methods ====================

    private String extractRefreshToken(String cookies) {
        String[] cookiesKeyValue = cookies.split(";");
        return Arrays.stream(cookiesKeyValue)
                .filter(cookie -> {
                    String key = cookie.split("=")[0].trim();
                    return key.equalsIgnoreCase("refresh_token");
                })
                .map(cookie -> cookie.split("=")[1].trim())
                .collect(Collectors.joining());
    }

    private void addTokenToCookie(TokenResponse token, HttpServletResponse response, String traceId) {
        Cookie cookie = new Cookie("refresh_token", token.getRefreshToken());
        cookie.setMaxAge((int) (token.getRefreshExpiresIn()));
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setAttribute("scope", token.getScope());
        cookie.setAttribute("samesite", sameSite.name());
        cookie.setPath("/");
        response.addCookie(cookie);
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());

        try {
            token.setRefreshToken(null);
            token.setRefreshExpiresIn(0);
            new ObjectMapper().writeValue(response.getOutputStream(), token);
        } catch (Exception e) {
            log.warn("Failed to write token to response. traceId: {}", traceId, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
