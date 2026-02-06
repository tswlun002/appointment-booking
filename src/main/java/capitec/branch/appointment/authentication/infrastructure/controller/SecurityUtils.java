package capitec.branch.appointment.authentication.infrastructure.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Component
public class SecurityUtils {

    public static String geEmailFromHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return request.getHeader("email");
    }

    public static String getUsernameFromHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return request.getHeader("username");
    }
    public boolean isUsernameMatching(Authentication authentication, String resourceUsername) {

        if (authentication == null || resourceUsername== null) {
            return false;
        }
        if( authentication.getPrincipal() instanceof DefaultOAuth2User principal ) {
            var userDetails = principal.getAttributes();
            var userUsername= userDetails.get("username");
            return userUsername!= null && userUsername.equals(resourceUsername);
        }
        return false;
    }

    public boolean isEmailMatching(Authentication authentication, String resourceEmail) {
        if (authentication == null || resourceEmail == null) {
            return false;
        }

        if( authentication.getPrincipal() instanceof DefaultOAuth2User principal ) {
            Map<String, Object> attributes = principal.getAttributes();
            var userEmail = attributes.get("email");
            return userEmail != null && userEmail.equals(resourceEmail);
        }
        return false;
    }
}
