package capitec.branch.appointment.validatecredentialmodule;

import capitec.branch.appointment.validatecredentialmodule.api.CustomPasswordValidateCurrentPassword;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;


public class CustomPasswordValidatorFactory implements RealmResourceProviderFactory {
    public static final String ID = "verify";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new CustomPasswordValidateCurrentPassword(session);
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void close() {}
}
