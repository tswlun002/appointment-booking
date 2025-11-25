package capitec.branch.appointment.keycloak.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;


@Slf4j
@Configuration
public class KeyCloakConfig {

    @Value("${keycloak.adminClientId}")
    private String adminClientId;
    @Value("${keycloak.adminClientSecret}")
    private String adminClientSecret;
    @Value("${keycloak.scope}")
    String keycloakScope;
    @Value("${keycloak.grant_type}")
    String keycloakGrantType;
    @Value("${keycloak.urls.auth}")
    private String authServerUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.password}")
    private String keycloakPassword;
    @Value("${keycloak.username}")
    private String keycloakUsername;

    @Bean
    public Keycloak keycloak() {
        Keycloak build = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId(adminClientId)
                .clientSecret(adminClientSecret)
                .username(keycloakUsername)
                .password(keycloakPassword)
                .scope(keycloakScope)
                .grantType(keycloakGrantType)
                .build();
        return build;
    }

    @Bean("adminCred")
    public   MultiValueMap<String, String> getAdminCred(){
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);
        formData.add("username", "admin");
        formData.add("password", "users");
        formData.add("scope", "openid profile email roles");
        return  formData;
    }

    @Bean("adminClientCred")
    public   MultiValueMap<String, String> getAdminClientCred(){

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", keycloakGrantType);
        params.add("client_id", adminClientId);
        params.add("client_secret", adminClientSecret);
        params.add("username", keycloakUsername);
        params.add("password", keycloakPassword);
        params.add("scope", keycloakScope);
        return  params;
    }

    @Bean("adminImpersonatingCred")
    public   MultiValueMap<String, String> getAdminImpersonatingCred(){
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        params.add("client_id", adminClientId);
        params.add("client_secret", adminClientSecret);
       // params.add("subject_token", adminToken);
       params.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
       // params.add("requested_subject", user.getUsername());
        params.add("scope","openid profile email");

        return  params;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Primary
    @Bean
    public RestClient restClient() {


        return RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                // .messageConverters(converters -> converters.add(new MyCustomMessageConverter()))
                .baseUrl(authServerUrl)
                //.defaultUriVariables(Map.of("variable", "foo"))
                //.defaultHeader("bearer", adminClientSecret)
                //.requestInterceptor(myCustomInterceptor)
                //.requestInitializer(myCustomInitializer)
                .build();
    }

    @Qualifier("general")
    @Bean
    public RestClient restClient1() {
        return RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                // .messageConverters(converters -> converters.add(new MyCustomMessageConverter()))
                //.defaultUriVariables(Map.of("variable", "foo"))
                //.defaultHeader("bearer", adminClientSecret)
                //.requestInterceptor(myCustomInterceptor)
                //.requestInitializer(myCustomInitializer)
                .build();
    }


}
