package capitec.branch.appointment.user.domain;

import capitec.branch.appointment.user.app.port.CapitecClientDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public record CreateUserExistingClientFactory(String email, String firstname, String lastname, String password) {
    static Logger logger = LoggerFactory.getLogger(CreateUserExistingClientFactory.class);

    public User createUser(Supplier<CapitecClientDetails> checkIfUserExists) {
        CapitecClientDetails clientDetails = checkIfUserExists.get();
        if (clientDetails == null) {
            throw new IllegalStateException("User client is not found");
        }
        if (!clientDetails.enabled()) {
            logger.warn("User client {} is disabled", clientDetails);
            throw new IllegalStateException("User client is blocked");
        }
        if (!(clientDetails.firstname().equals(firstname) || clientDetails.lastname().equals(lastname) || clientDetails.email().equals(email))) {
            logger.info("User information doesn't match with existing user, input data, username:{}", clientDetails.username());
            throw new IllegalStateException("User information doesn't match with existing user");
        }
        return new User(clientDetails.username(), clientDetails.email(), clientDetails.firstname(), clientDetails.lastname(),
                password, false, clientDetails.enabled());
    }
}
