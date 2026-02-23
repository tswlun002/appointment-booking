package capitec.branch.appointment.generateusername.listener;

import java.lang.Exception;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;


/**
 * Listens for the REGISTER event and updates the user's username using
 * a custom generation logic.
 */
public class CustomUsernameGeneratorListener implements EventListenerProvider {
    private  final  String REGEX = "^(?!0)(?!.*(\\d)\\1{2})\\d{10}$";

    private  final KeycloakSession session;

    private static final Logger logger = LoggerFactory.getLogger(CustomUsernameGeneratorListener.class);

    public CustomUsernameGeneratorListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        logger.info("Event received {}", event.getType());
        if (event.getType() == EventType.REGISTER) {
            String userId = event.getUserId();
            logger.info("Registered client {} successfully",userId);
        }
    }

    private void handleUserRegistration(String realmId,String userId) {
        RealmModel realm = session.realms().getRealm(realmId);
        UserProvider userProvider = session.users();



        UserModel user = userProvider.getUserById(realm, userId);

        if (user == null) {
            logger.warn("Registered user not found for ID: {}" ,userId);
            return;
        }

        // Skip clients because their username are already generated at registration level
        else if(! user.getUsername().equals(user.getEmail()) || user.getUsername().matches(REGEX)) {

            logger.info("User {} already exists for email {}",user.getUsername(),user.getEmail());
            return;
        }


        try {
            String newUsername = generateUniqueUsername(user);
            // Set the new, custom username
            user.setUsername(newUsername);
            logger.debug("Updated username for user ID:{} to {}", user.getId(), newUsername);
        } catch (Exception e) {
           logger.error("Failed to update username for user register in keycloak UI, user email:{} " ,user.getUsername());

        }


    }

    private String generateUniqueUsername(UserModel user) {


        try {
            // 1. Construct the API request
            var uuid = UUID.randomUUID().toString();
            logger.info("Generating unique username for user ID: " + uuid);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://appointment-booking:8083:/api/v1/users/auth/generate/username"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Trace-Id", uuid)
                    .GET()
                    .build();

            // 2. Send the request
            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 3. Process the response
            if (response.statusCode() == 200) {
                // Use response data to generate or verify the username

                return response.body();

            }

            logger.warn("Failed to generate username for user ID:{}" , user.getUsername());
            return null;


        } catch (Exception e) {
            logger.info("Failed to call external API: {}" , e.getMessage());
            throw new InternalError("Failed to call external API: " + e.getMessage());
        }


    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        // Not used for user registration events
        OperationType operationType = adminEvent.getOperationType();
        logger.info("Admin event received {}", operationType.name());

        ResourceType resourceType = adminEvent.getResourceType();
        if(operationType == OperationType.CREATE &&  resourceType == ResourceType.USER) {
            String resourcePath = adminEvent.getResourcePath();
            String userId = resourcePath.substring(resourcePath.lastIndexOf("/")+1);

            handleUserRegistration(adminEvent.getRealmId(), userId);
        }

    }

    @Override
    public void close() {
        // Clean up resources if necessary
    }
}