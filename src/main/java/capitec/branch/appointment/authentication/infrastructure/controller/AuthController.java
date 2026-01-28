package capitec.branch.appointment.authentication.infrastructure.controller;

import capitec.branch.appointment.authentication.app.LoginDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import capitec.branch.appointment.authentication.domain.AuthUseCase;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

    private final AuthUseCase authUseCase;

    /**
     * User login.
     *
     * @param loginDTO the login credentials
     * @param traceId  unique trace identifier for request tracking
     * @param response HTTP response for setting cookies
     */
    @PostMapping("/login")
    public void login(@RequestBody LoginDTO loginDTO, @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {
        log.info("User signing in, traceId:{}", traceId);
        TokenResponse login = authUseCase.login(loginDTO, traceId);
        addTokenToCookie(login, response, traceId);
    }

    /**
     * Refresh access token using refresh token from cookie.
     *
     * @param cookies  cookies header containing refresh token
     * @param traceId  unique trace identifier for request tracking
     * @param response HTTP response for setting new cookies
     */
    @PostMapping("/refresh")
    public void refresh(@RequestHeader(HttpHeaders.COOKIE) String cookies, @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {
        String[] cookiesKeyValue = cookies.split(";");
        var refreshToken = Arrays.stream(cookiesKeyValue)
                .filter(cookie -> {
                    String s = cookie.split("=")[0].trim();
                    return s.equalsIgnoreCase("refresh_token");
                })
                .map(cookie -> cookie.split("=")[1].trim())
                .collect(Collectors.joining());

        log.info("Refresh user token, traceId:{}", traceId);

        TokenResponse tokenResponse = authUseCase.refreshAccessToken(refreshToken, traceId);
        addTokenToCookie(tokenResponse, response, traceId);
    }

    /**
     * Verify user's current password.
     *
     * @param userCredentialDTO the user credentials
     * @param traceId           unique trace identifier for request tracking
     * @return validation result
     */
    @PostMapping("/credentials/password/verify")
    public ResponseEntity<?> verifyUserCurrentPassword(@RequestBody UserCredentialDTO userCredentialDTO, @RequestHeader("Trace-Id") String traceId) {
        boolean isValid = authUseCase.verifyCurrentPassword(userCredentialDTO.username(), userCredentialDTO.Password(), traceId);
        return new ResponseEntity<>(isValid ? "Password is valid." : "Invalid credentials", isValid ? HttpStatus.OK : HttpStatus.UNAUTHORIZED);
    }

    /**
     * Admin impersonate user login.
     *
     * @param username the username to impersonate
     * @param traceId  unique trace identifier for request tracking
     * @return token response
     */
    @PostMapping("/admin/impersonate/{username}")
    @PreAuthorize("hasAnyRole('impersonate') and hasAnyRole('admin')")
    public ResponseEntity<TokenResponse> adminImpersonateUser(@PathVariable("username") String username, @RequestHeader("Trace-Id") String traceId) {
        TokenResponse tokenResponse = authUseCase.adminImpersonateUser(username, traceId);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * User logout.
     *
     * @param cookies  cookies header containing refresh token
     * @param traceId  unique trace identifier for request tracking
     */
    @PostMapping("/logout")
    public void logout(@RequestHeader(HttpHeaders.COOKIE) String cookies, @RequestHeader("Trace-Id") String traceId) {
        String[] cookiesKeyValue = cookies.split(";");
        var refreshToken = Arrays.stream(cookiesKeyValue)
                .filter(cookie -> {
                    String s = cookie.split("=")[0].trim();
                    return s.equalsIgnoreCase("refresh_token");
                })
                .map(cookie -> cookie.split("=")[1].trim())
                .collect(Collectors.joining());

        log.info("User signing out, traceId:{}", traceId);
        authUseCase.logout(refreshToken, traceId);
    }

    private void addTokenToCookie(TokenResponse token, HttpServletResponse response, String traceId) {
        // Add refresh token to cookies and secure it
        Cookie cookie = new Cookie("refresh_token", token.getRefreshToken());
        cookie.setMaxAge((int) (token.getRefreshExpiresIn()));
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setAttribute("scope", token.getScope());
        cookie.setPath("/");
        response.addCookie(cookie);

        // Add access token to response body
        Map<String, TokenResponse> tokens = new HashMap<>();
        tokens.put("access_token", token);

        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        try {
            token.setRefreshToken(null);
            token.setRefreshExpiresIn(0);
            new ObjectMapper().writeValue(response.getOutputStream(), tokens);
        } catch (IOException e) {
            log.error("Failed to write token to response, traceId:{}", traceId, e);
            throw new InternalServerErrorException("Internal Server Error");
        }
    }
}
