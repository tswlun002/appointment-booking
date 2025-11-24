package lunga.appointmentbooking.authentication.infrastructure.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import lunga.appointmentbooking.authentication.domain.AuthUseCase;
import lunga.appointmentbooking.authentication.domain.TokenResponse;
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

@RestController
@Slf4j
@RequestMapping("authentication-service")
@RequiredArgsConstructor
public class AuthController {

    private  final AuthUseCase authUseCase;
    @PostMapping("/auth/login")
    public void loging(@RequestBody LoginDTO loginDTO, @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {

        log.info("User signing in, traceId:{}",traceId);
        TokenResponse login = authUseCase.login(loginDTO, traceId);
        addTokenToCookie(login, response, traceId);
    }
    @PostMapping("/auth/refresh/token")
    public void refresh(@RequestHeader(HttpHeaders.COOKIE) String cookies, @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {

        String[] cookiesKeyValue = cookies.split(";");
        var refreshToken = Arrays.stream(cookiesKeyValue).filter(cookie->
                {
                    String s = cookie.split("=")[0].trim();
                    boolean b = s.equalsIgnoreCase("refresh_token");
                    log.info("Refresh token:{}",b);
                    return b;
                }
        ).map(cookie->cookie.split("=")[1].trim()).collect(Collectors.joining()) ;

        log.info("Refresh user token:{} signing in, traceId:{}",refreshToken,traceId);

        TokenResponse tokenResponse = authUseCase.refreshAccessToken(refreshToken, traceId);
        addTokenToCookie(tokenResponse, response, traceId);
    }

    @PostMapping("/credentials/password/verify")
    public ResponseEntity<?> verifyUserCurrentPassword(@RequestBody UserCredentialDTO userCredentialDTO, @RequestHeader("Trace-Id") String traceId) {

        boolean isValid = authUseCase.verifyCurrentPassword(userCredentialDTO.username(), userCredentialDTO.Password(), traceId);

        return  new ResponseEntity<>(isValid?"Password is valid.":"Invalid credentials", isValid?HttpStatus.OK:HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/admin/impersonate/user/login/{username}")
    @PreAuthorize("hasAnyRole('impersonate') and hasAnyRole('admin')")
    public ResponseEntity<TokenResponse> adminImpersonateUser(@PathVariable("username")String username, @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {

        TokenResponse tokenResponse = authUseCase.adminImpersonateUser(username, traceId);

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    @PreAuthorize("#securityUtils.usernameFromHeader==#username")
    public void logout(@RequestHeader(HttpHeaders.COOKIE) String refreshToken, @RequestHeader("Trace-Id") String traceId, HttpServletResponse response) {

       /* log.info("User:{} logging out, traceId:{}", username,traceId);

        String[] cookiesKeyValue = cookies.split(";");
        var refreshToken = Arrays.stream(cookiesKeyValue).filter(cookie->
                {
                    String s = cookie.split("=")[0].trim();
                    boolean b = s.equalsIgnoreCase("refresh_token");
                    log.info("Refresh token:{}",b);
                    return b;
                }
        ).map(cookie->cookie.split("=")[1].trim()).collect(Collectors.joining()) ;*/

        log.info("Refresh user token:{} sign out , traceId:{}",refreshToken,traceId);

        authUseCase.logout(refreshToken,traceId);
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
        Map<String, TokenResponse> tokens = new HashMap<>();
        tokens.put("access_token", token);

        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        try {

            token.setRefreshToken(null);
            token.setRefreshExpiresIn(0);
            new ObjectMapper().writeValue(response.getOutputStream(), tokens);

        } catch (IOException e) {
            log.error("Failed to write token to response, traceId:{}", traceId,e);
            throw new InternalServerErrorException("Internal Server Error");
        }
    }
}
