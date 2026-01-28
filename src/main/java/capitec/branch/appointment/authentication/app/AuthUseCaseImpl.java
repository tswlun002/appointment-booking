package capitec.branch.appointment.authentication.app;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import capitec.branch.appointment.authentication.domain.AuthService;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import capitec.branch.appointment.authentication.domain.AuthUseCase;
import capitec.branch.appointment.utils.UseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.text.ParseException;
import java.util.Map;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class AuthUseCaseImpl implements AuthUseCase {

    private final AuthService authService;


    public  Map<String, Object> getClaims(String token) {

        try {
            JWTClaimsSet jwtClaimsSet = SignedJWT.parse(token).getJWTClaimsSet();
            return jwtClaimsSet.getClaims();

        }catch (ParseException e){

            log.error("Failed to convert token string  to JWT object\n",e);
            throw new InternalServerErrorException("Internal Server Error");
        }
    }

    private  void  validateUser(String token,String traceId) {

        Map<String, Object> claims = getClaims(token);

        if(!((Boolean)claims.get("email_verified"))) {

            log.error("User is not verified, traceId:{}", traceId);
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "User is not verified, please verify your email first");
        }
    }
    @Override
    public TokenResponse login(LoginDTO loginDTO, String traceId) {

        log.info("User signing in, traceId:{}", traceId);

        var token = authService.login(loginDTO.email(), loginDTO.password(), traceId)
                .orElseThrow(() -> {

                    log.error("User:{} failed to login, traceId:{}",loginDTO.email(), traceId);
                    return new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Login failed, please try again");
                });

        log.debug("User signed in, traceId:{}, token:{}", traceId, token.toString());
        validateUser(token.getToken(), traceId);

        return token;
    }

    @Override
    public TokenResponse refreshAccessToken(String refreshToken, String traceId) {

        log.info("User refreshing access token, traceId:{}",traceId);

        var token = authService.refreshAccessToken(refreshToken, traceId)
                .orElseThrow(() -> {

                    log.error("User failed to refresh access token, traceId:{}",traceId);
                    return new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Login failed, please try again");
                });

        validateUser(token.getToken(), traceId);

       return token;

    }

    @Override
    public boolean verifyCurrentPassword(String username, String password,String traceId) {

        log.info("User verifying password, traceId:{}", traceId);

       return authService.verifyCurrentPassword(username, password, traceId);
    }
    
    @Override
    public  void logout(String refreshToken,String traceId) {

        log.info("User log out, traceId:{}",traceId);
        authService.logout(refreshToken, traceId);

    }

  @Override
    public TokenResponse adminImpersonateUser(String username,String traceId) {

        log.info("Admin impersonating user, traceId:{}", traceId);

        return authService.impersonateUser(username)
                .orElseThrow(() -> {

                    log.error("User failed to impersonate user, traceId:{}", traceId);
                    return new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Login failed, please try again");
                });
    }

}
