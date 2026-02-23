package capitec.branch.appointment.keycloak.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public class KeycloakRestClient {

    private final RestClient restClient;

    public KeycloakRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public <T> Optional<T> postFormUrlEncoded(String url, MultiValueMap<String, String> body, Class<T> responseType) {
        return restClient.post()
                .uri(url)
                .body(body)
                .headers(this::setFormHeaders)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .exchange((req, res) -> {
                    if (res.getStatusCode().is2xxSuccessful()) {
                        return Optional.ofNullable(res.bodyTo(responseType));
                    }
                    log.error("Request failed: {}", res.getStatusText());
                    throw new ResponseStatusException(res.getStatusCode(), res.getStatusText());
                });
    }

    public boolean postJsonForVerification(String url, Object body, String bearerToken, String traceId) {
        return restClient.post()
                .uri(url)
                .body(body)
                .headers(h -> setJsonHeaders(h, bearerToken, traceId))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange((req, res) -> {
                    if (res.getStatusCode().is2xxSuccessful()) {
                        log.info("Successfully verified user password");
                        return true;
                    }
                    if (res.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                        log.info("Unauthorized user password, Exception trace: {}", res.bodyTo(String.class));
                        return false;
                    }
                    log.error("Failed to verify user password, request: {}, response: {}", req, res);
                    throw new ResponseStatusException(res.getStatusCode(), res.getStatusText());
                });
    }

    private void setFormHeaders(HttpHeaders headers) {
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        headers.add("Accept-Encoding", "gzip, deflate, br");
        headers.add("User-Agent", "Mozilla/5.0");
        headers.add(HttpHeaders.ACCEPT, MediaType.ALL_VALUE);
    }

    private void setJsonHeaders(HttpHeaders headers, String bearerToken, String traceId) {
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.add("Trace-Id", traceId);
    }
}
