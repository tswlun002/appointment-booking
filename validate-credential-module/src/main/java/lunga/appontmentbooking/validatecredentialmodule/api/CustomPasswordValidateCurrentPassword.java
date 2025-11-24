package lunga.appontmentbooking.validatecredentialmodule.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.*;
import org.keycloak.services.resource.RealmResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;


public class CustomPasswordValidateCurrentPassword implements RealmResourceProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomPasswordValidateCurrentPassword.class);

    private final KeycloakSession session;

    public CustomPasswordValidateCurrentPassword(KeycloakSession session) {
        this.session = session;
    }
    @POST
    @Path("password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validatePassword(@Context HttpHeaders headers, Map<String, String> payload) {

        String traceId = headers.getHeaderString("Trace-Id");
        String username = payload.get("username");
        String password = payload.get("password");
        RealmModel realm = session.getContext().getRealm();
        UserProvider users = session.users();
        UserModel user = users.getUserByUsername(realm, username);

        // NOTE: For a real deployment, you should ensure this method of verification
        // is secure and aligns with Keycloak's expected authentication flow.

        if (user == null) {
            log.warn("User {} not found, traceId:{}", username, traceId);
            return Response.status(Response.Status.UNAUTHORIZED).entity(Collections.singletonMap("error", "Invalid credentials")).build();
        }

        if ( user.credentialManager().isValid(UserCredentialModel.password(password))) {
            log.info("User {} is valid, traceId:{}", username, traceId);
            return Response.ok(Collections.singletonMap("message", "Password verified")).build();
        } else {
            log.warn("User {} password is not valid, traceId:{}", username, traceId);
            return Response.status(Response.Status.UNAUTHORIZED).entity(Collections.singletonMap("error", "Invalid password")).build();
        }

    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }
}