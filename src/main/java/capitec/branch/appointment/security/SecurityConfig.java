package capitec.branch.appointment.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${allowed_origins.urls}")
    private Set<String> allowedOrigins;
    @Value("${allowed_origins.cache_period}")
    private long allowedOriginCachePeriod;

    private final KeycloakRoleConverter keycloakRoleConverter;


    public SecurityConfig(KeycloakRoleConverter keycloakRoleConverter) {
        this.keycloakRoleConverter = keycloakRoleConverter;
    }

    @Bean
    @Primary
    public SecurityFilterChain securityWebFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(configCORS())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(configSecurityAuthority())
                .oauth2ResourceServer(oauth2 ->{
                     oauth2.jwt(jwtConfigurer ->{
                         var jwtConv = new JwtAuthenticationConverter();
                         jwtConv.setJwtGrantedAuthoritiesConverter(keycloakRoleConverter);

                         jwtConfigurer.jwtAuthenticationConverter(jwtConv);

                    });
                });

        return httpSecurity.build();
    }



    private Customizer<CorsConfigurer<HttpSecurity>> configCORS() {
        return serverHttpSecurity -> serverHttpSecurity.configurationSource(exchange -> {
            CorsConfiguration corsConfiguration = new CorsConfiguration();
            corsConfiguration.setAllowedOrigins(allowedOrigins.stream().toList());
            corsConfiguration.setAllowCredentials(true);
            corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
            corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
            corsConfiguration.setMaxAge(Duration.ofMinutes(allowedOriginCachePeriod));
            return corsConfiguration;
        });

    }

    private Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> configSecurityAuthority() {


        return exchange -> {

            exchange
                    .requestMatchers("/authentication-service/auth/*").permitAll()
                    .requestMatchers("/users-service/auth/*").permitAll()
                    .requestMatchers("/actuator/*").permitAll()
                    .anyRequest().authenticated();
        };
    }



}
