package capitec.branch.appointment.user.infrastructure.keycloak;

import capitec.branch.appointment.keycloak.domain.KeycloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static capitec.branch.appointment.user.infrastructure.keycloak.KeycloakUserCacheConfig.KEYCLOAK_USER_CACHE;
import static capitec.branch.appointment.user.infrastructure.keycloak.KeycloakUserCacheConfig.KEYCLOAK_USER_CACHE_MANAGER;
import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;

/**
 * Shared helper for Keycloak user operations.
 * Provides caching for read operations to reduce Keycloak calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakUserHelper {


    private final KeycloakService keycloakService;

    public UsersResource getUsersResource() {
        return keycloakService.getUsersResources();
    }

    @Cacheable(value = KEYCLOAK_USER_CACHE, key = "'username:' + #username",
               cacheManager = KEYCLOAK_USER_CACHE_MANAGER, unless = "#result == null")
    public Optional<UserRepresentation> findByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            log.debug("Username is blank, returning empty");
            return Optional.empty();
        }

        log.debug("Fetching user from Keycloak. username: {}", username);

        return keyCloakRequest(
                () -> getUsersResource().searchByUsername(username, true)
                        .stream()
                        .findFirst(),
                "find user by username",
                UserRepresentation.class
        );
    }

    @Cacheable(value = KEYCLOAK_USER_CACHE, key = "'email:' + #email",
               cacheManager = KEYCLOAK_USER_CACHE_MANAGER, unless = "#result == null")
    public Optional<UserRepresentation> findByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            log.debug("Email is blank, returning empty");
            return Optional.empty();
        }

        log.debug("Fetching user from Keycloak. email: {}", email);

        return keyCloakRequest(
                () -> getUsersResource().searchByEmail(email, true)
                        .stream()
                        .findFirst(),
                "find user by email",
                UserRepresentation.class
        );
    }

    public UserRepresentation findByUsernameOrThrow(String username) {
        return findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found. username: {}", username);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });
    }

    public UserResource getUserResource(String username) {
        UserRepresentation userRep = findByUsernameOrThrow(username);
        return getUsersResource().get(userRep.getId());
    }

    public Optional<UserResource> findUserResource(String username) {
        return findByUsername(username)
                .map(userRep -> getUsersResource().get(userRep.getId()));
    }

    /**
     * Evict user from cache after modifications (update, delete, etc.)
     */
    @CacheEvict(value = KEYCLOAK_USER_CACHE, cacheManager = KEYCLOAK_USER_CACHE_MANAGER, allEntries = true)
    public void evictUserCache(String username) {
        log.debug("Evicting user cache. username: {}", username);
    }

    /**
     * Evict all user cache entries.
     */
    @CacheEvict(value = KEYCLOAK_USER_CACHE, cacheManager = KEYCLOAK_USER_CACHE_MANAGER, allEntries = true)
    public void evictAllUserCache() {
        log.debug("Evicting all user cache entries");
    }
}
