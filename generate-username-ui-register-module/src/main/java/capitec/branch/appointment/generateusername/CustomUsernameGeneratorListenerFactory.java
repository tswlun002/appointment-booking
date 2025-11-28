package capitec.branch.appointment.generateusername;

import capitec.branch.appointment.generateusername.listener.CustomUsernameGeneratorListener;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

/**
 * Factory class for the CustomUserRegistrationListener.
 * Keycloak uses this factory to initialize and configure the EventListenerProvider.
 */
public class CustomUsernameGeneratorListenerFactory implements EventListenerProviderFactory {

    public static final String ID = "custom-username-generator-listener";


    @Override
    public EventListenerProvider create(KeycloakSession session) {

        return new CustomUsernameGeneratorListener(session);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void init(org.keycloak.Config.Scope config) {

    }
    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        // Optional: Define configuration properties if your listener needs custom settings.
        return Collections.emptyList();
    }
}