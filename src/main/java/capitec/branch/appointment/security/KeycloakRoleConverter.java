package capitec.branch.appointment.security;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Collection;
import java.util.stream.Collectors;

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
        List<String> rolesString = source.getClaim("roles");

        if (rolesString == null || rolesString.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<GrantedAuthority> roles = rolesString.stream().map(
                role->new SimpleGrantedAuthority("ROLE_" + role)
        ).collect(Collectors.toSet());;


        return roles;
    }
}
