package capitec.branch.appointment.security;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt source) {
        log.debug("Converting Jwt to AbstractAuthenticationToken");

        Collection<GrantedAuthority> authorities = new HashSet<>();

        authorities.addAll(new JwtGrantedAuthoritiesConverter().convert(source));

        authorities.addAll(extractResourceRoles(source));

        return authorities;
    }

    private Collection<GrantedAuthority> extractResourceRoles(Jwt source) {
        Map<String, Object> resourceAccess = source.getClaim("resource_access");

        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<GrantedAuthority> roles = new HashSet<>();

        // Extract roles from all clients
        resourceAccess.forEach((client, resource) -> {
            if (resource instanceof Map) {
                Map<String, Object> resourceMap = (Map<String, Object>) resource;
                Object rolesObj = resourceMap.get("roles");

                if (rolesObj instanceof Collection) {
                    ((Collection<String>) rolesObj).forEach(role ->
                            roles.add(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                }
            }
        });

        return roles;
    }
}
