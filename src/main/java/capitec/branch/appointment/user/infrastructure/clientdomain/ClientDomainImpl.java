package capitec.branch.appointment.user.infrastructure.clientdomain;


import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.user.domain.ClientDomain;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.user.infrastructure.UserMapperReflection;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

@Service

@Slf4j
public class ClientDomainImpl implements ClientDomain {


    private final RestClient clientDomainClient;

    private final KeycloakService keycloakService;
    private final UserMapperReflection userMapper;

    public ClientDomainImpl( @Qualifier("clientDomainRestClient")RestClient clientDomainClient, KeycloakService keycloakService, UserMapperReflection userMapper) {
        this.clientDomainClient = clientDomainClient;
        this.keycloakService = keycloakService;
        this.userMapper = userMapper;

    }

    @Override
    public Optional<User> findByUsername(String IDNumber) {


        //Assume admin has role to call client api
        AccessTokenResponse accessToken = keycloakService.getTokenManager().getAccessToken();

        return  clientDomainClient.get()
                .uri("/v1/clients?IDNumber=" + IDNumber)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getToken())
                .exchange((_, response) -> {

                    if (HttpStatusCode.valueOf(response.getStatusCode().value()).is2xxSuccessful()) {

                        log.debug("User found with IDNumber: {}", IDNumber);
                        return Optional.of(Objects.requireNonNull(userMapper.mapFromInputStream(response.getBody())));
                    } else if (HttpStatus.NOT_FOUND == response.getStatusCode()) {

                        log.debug("User not found with IIDNumber: {}, error:[ status {} , statusText:{}]", IDNumber, response.getStatusCode(), response.getStatusText());
                        return Optional.empty();
                    } else {

                        log.error("Unexpected error while trying to find user with IDNumber: {}, error:[ status {} , statusText:{}]", IDNumber, response.getStatusCode(), response.getStatusText());
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
                    }

                });
    }
}
