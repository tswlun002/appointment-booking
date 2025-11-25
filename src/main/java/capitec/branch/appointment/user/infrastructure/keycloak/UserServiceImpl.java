package capitec.branch.appointment.user.infrastructure.keycloak;


import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.otp.domain.FailedCreateOTPEvent;
import capitec.branch.appointment.role.domain.RolesAndGroupsService;
import capitec.branch.appointment.user.domain.ResetPasswordService;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.user.domain.UserRoleService;
import capitec.branch.appointment.user.domain.UserService;
import capitec.branch.appointment.user.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;


@Slf4j
@Component
public class UserServiceImpl implements UserService, UserRoleService, ResetPasswordService {

    private final String authType;
    private final RolesAndGroupsService rolesAndGroupsService;
    private  final KeycloakService keycloakService;
   // private final AuthUseCase authenticationService;

    public UserServiceImpl(@Value("${keycloak.user_auth_type}") String authType, RolesAndGroupsService rolesAndGroupsService, KeycloakService keycloakService
                           //AuthUseCase authenticationService
    ) {
        this.authType = authType;
        this.rolesAndGroupsService = rolesAndGroupsService;
       // this.authenticationService = authenticationService;
        this.keycloakService = keycloakService;

    }
    @Valid
    @Override
    public User registerUser(@Valid final User user) {

        var credentialRepresentation = getCredentialRepresentation(user);
        var keyCloakUser = getUserRepresentation(user, user.isEnabled(), user.isVerified(), credentialRepresentation);

        var response= keyCloakRequest(()->keycloakService.getUsersResources().create(keyCloakUser)," register user ", User.class);
        var status = HttpStatus.valueOf(response.getStatus());

        if (status.is5xxServerError()) {
            log.error("Failed to save user:{}, manual rollback is needed, response status:{}, response message:{}, response headers:{}",
                    user, response.getStatus(), response.getStatusInfo(), response.getHeaders());
            throw new InternalServerErrorException("Internal server error", response);
        }
        if (status == HttpStatus.CONFLICT) {
            log.info("A verified User:{} already exists", user.getUsername());
            throw new EntityAlreadyExistException("User already exist.");
        }
        if (status.is2xxSuccessful()) {
            return user;
        }
        else {
            log.error("Unexpected response:{}.\nFailed to save user", response);
            throw new InternalServerErrorException("Internal server error");
        }
    }


    @EventListener(FailedCreateOTPEvent.class)
    public void rollBackSavedUserByEmail(FailedCreateOTPEvent event) {

        var username = event.username();
        var traceId = event.traceId();
        if (deleteUser(username)) {
            log.info("Rollback saved user:{}, traceId;{}", username, traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to create user");
        } else {
            log.info("Rollback failed for user:{} failed, traceId;{}", username, traceId);
            throw new InternalServerErrorException("Internal server error");
        }

    }

    @Override
    public boolean deleteUser(String username) {


        var user = getUserRepresentationByUsername(keycloakService.getUsersResources(),username).orElseThrow(()->{
            log.error("User not found with Username:{}", username);
            return   new NotFoundException("User not found");
        });

        String serviceAccountClientId = user.getId();
        var isDeleted = false;

        Response response = keyCloakRequest(()->keycloakService.getUsersResources().delete(serviceAccountClientId)," delete user ", User.class);

        HttpStatusCode httpStatusCode = HttpStatusCode.valueOf(response.getStatus());

        if (httpStatusCode.is2xxSuccessful()) {
            isDeleted = true;
            log.info("Successfully deleted user:{}", user);
        } else if (HttpStatusCode.valueOf(response.getStatus()).is5xxServerError()) {
            log.error("Keycloak service is down, connections failed.{}", response);
            throw new InternalServerErrorException("Sorry for inconvenience. Our service is down please try again later.");
        } else {
            log.error("Failed to delete user:{}, response:{}", user, response);
            throw new ResponseStatusException(HttpStatus.valueOf(response.getStatus()), response.getStatusInfo().getReasonPhrase());
        }

        return isDeleted;
    }

    @Override
    public boolean checkIfUserExists(String username) {
        return getUserByUsername(username).isPresent();
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        var userRepresentation = getUserRepresentationByEmail(keycloakService.getUsersResources(),email);
        return userRepresentation.map(UserMapperReflection::mapToUser);
    }

    @Override
    public void updateUseStatus(String username, Boolean useStatus) {
        keyCloakRequest(() -> {

            UserRepresentation representation = getUserRepresentationByUsername(keycloakService.getUsersResources(),username).orElseThrow();
            representation.setEnabled(useStatus);
            UserResource userResource = keycloakService.getUsersResources().get(representation.getId());
            userResource.update(representation);
            log.info("Successfully updated user status");
            return true;
        }, "Updated user  status", User.class);
    }

    private Optional<UserRepresentation> getUserRepresentationByEmail(UsersResource usersResource,String email) {

       return keyCloakRequest(()->usersResource.searchByEmail(email,true).stream().findFirst()," get user ",UserRepresentation.class);
    }

    @Override
    public boolean verifyUser(String  username) {

        return  keyCloakRequest(()->{
            UsersResource usersResources = keycloakService.getUsersResources();
            UserRepresentation representation = getUserRepresentationByUsername(usersResources,username).orElseThrow();
            representation.setEmailVerified(true);
            representation.setRequiredActions(List.of());
            UserResource userResource = usersResources.get(representation.getId());
            userResource.update(representation);
            log.debug("Successfully verified user:{}", username);
           return true;
       }, "verify user Username",User.class);

    }

    @Override
    public boolean verifyUserCurrentPassword(String  username, String password) {
        UserRepresentation userRepresentation = getUserRepresentationByUsername(keycloakService.getUsersResources(),username).orElseThrow();
        return   keycloakService.verifyUserPassword(userRepresentation.getUsername(),password);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {

        var userRep = getUserRepresentationByUsername(keycloakService.getUsersResources(),username);
        return userRep.map(UserMapperReflection::mapToUser);
    }

    @NonNull
    private CredentialRepresentation getCredentialRepresentation(User user) {
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(AuthType.fromAuthType(authType).name());
        credentialRepresentation.setTemporary(Boolean.FALSE);
        credentialRepresentation.setValue(user.getPassword());
        return credentialRepresentation;
    }

    @NotNull
    private static UserRepresentation getUserRepresentation(User user, boolean isEnabled, boolean isVerified, CredentialRepresentation credentialRepresentation) {
        var credentials = List.of(credentialRepresentation);
        var keyCloakUser = new UserRepresentation();
        keyCloakUser.setUsername(user.getUsername());
        keyCloakUser.setEmail(user.getEmail());
        keyCloakUser.setFirstName(user.getFirstname());
        keyCloakUser.setLastName(user.getLastname());
        keyCloakUser.setEnabled(isEnabled);
        keyCloakUser.setEmailVerified(isVerified);
        keyCloakUser.setCredentials(credentials);
        keyCloakUser.setRequiredActions(List.of("VERIFY_EMAIL"));

        var zone = ZoneOffset.systemDefault();
        keyCloakUser.setCreatedTimestamp(LocalDateTime.now().atZone(zone).toInstant().toEpochMilli());
        return keyCloakUser;
    }


    private Optional<UserRepresentation> getUserRepresentationByUsername(UsersResource  usersResource,String username) {

       return keyCloakRequest(()->

           usersResource.searchByUsername(username,true).stream().findFirst()

        , "find user by Username",User.class);

    }

    private  Optional<UserResource> getUserResourceByUsername( UsersResource usersResource,String username) {

        return keyCloakRequest(()->{
                    var userRepresentationByusername = getUserRepresentationByUsername(usersResource,username)
                            .orElseThrow(()->{
                                log.warn("User not found for Username:{}", username);
                                return new NotFoundException("User not found");
                            });
                    UserResource userResource = usersResource.get(userRepresentationByusername.getId());

                    if (userResource == null) {
                        return Optional.empty();
                    }
                    return Optional.of(userResource);
                },
                "get user",User.class);

    }

    @Override
    public boolean assignRoleToUser( String username,String roleId) {

        UserResource userResource = getUserResourceByUsername(keycloakService.getUsersResources(),username).orElseThrow(() -> {
            log.error("User  not found with eventId {}", username);
            return new NotFoundException("User not found");
        });



        var role =rolesAndGroupsService.getClientRoleById(roleId).orElseThrow(()->{
                    log.warn("Role not found with eventId {}", roleId);
                    return new NotFoundException("Role not found");
                });

        return keyCloakRequest(()->{

            ClientRepresentation clientRepresentation = keycloakService.getClientRep().orElseThrow();

            RoleScopeResource roleScopeResource = userResource.roles().clientLevel(clientRepresentation.getId());
           RoleRepresentation roleRepresentation = new RoleRepresentation(role.getName(), role.getDescription(), role.isClientRole());
           roleRepresentation.setId(roleId);
           roleScopeResource.add(List.of(roleRepresentation));

           return true;

        }, "assign role to user",User.class);

    }

    @Override
    public Collection<String> getUserRoles(String username) {



        UserResource userResource = getUserResourceByUsername(keycloakService.getUsersResources(),username).orElseThrow(()->{
            log.error("User  not found with eventId {}", username);
            return new NotFoundException("User not found");
        });

        return keyCloakRequest(()->

                {
                    ClientRepresentation clientRepresentation = keycloakService.getClientRep().orElseThrow();

                    return userResource.roles().clientLevel(clientRepresentation.getId())
                    .listEffective()
                    .stream().map(RoleRepresentation::getId)
                    .collect(Collectors.toSet());
                },

                "get client role for user:"+username, RoleRepresentation.class);
    }

    @Override
    public boolean addUserToGroup(String username,String groupId) {

        UserResource userResource = getUserResourceByUsername(keycloakService.getUsersResources(),username).orElseThrow(() -> {
            log.error("User  not found with username {}", username);
            return new NotFoundException("User not found");
        });

        if(rolesAndGroupsService.getGroupById(groupId).isEmpty()) {
            log.error("Group {} does not exists", groupId);
            throw  new NotFoundException("Group not found");
        }

        return keyCloakRequest(()-> {
                     userResource.joinGroup(groupId);
                     return true ;
                 }
        , "assign user to group",User.class);
    }

    @Override
    public Collection<String> getUserGroups(String username,int first, int last, boolean briefly) {

        UserResource userResource = getUserResourceByUsername(keycloakService.getUsersResources(),username).orElseThrow(() -> {
            log.error("User  not found with username {}", username);
            return new NotFoundException("User not found");
        });

        return keyCloakRequest(()->

            userResource.groups(first, last, briefly).stream().map(GroupRepresentation::getId).collect(Collectors.toSet())

        , " get user groups ",GroupRepresentation.class);
    }

    @Override
    public boolean passwordReset(@Valid User user) {

        UserResource userResource = getUserResourceByUsername(keycloakService.getUsersResources(),user.getUsername()).orElseThrow();

        return keyCloakRequest(()->{
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setValue(user.getPassword());
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setTemporary(Boolean.FALSE);
            userResource.resetPassword(credentialRepresentation);

            return true;
        }, " reset password ", User.class);
    }


}
