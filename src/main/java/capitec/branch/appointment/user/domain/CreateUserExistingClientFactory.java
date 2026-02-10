package capitec.branch.appointment.user.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public record CreateUserExistingClientFactory( String email, String firstname, String lastname, String password) {
      static Logger logger = LoggerFactory.getLogger(CreateUserExistingClientFactory.class);
    public  User createUser(Supplier<UserClientDetails> checkIfUserExists) {
        UserClientDetails userClientDetails = checkIfUserExists.get();
        if(userClientDetails ==null) {
            throw new IllegalStateException("User client is not found");
        }
        if( !userClientDetails.enabled()){
            logger.warn("User client {} is disabled", userClientDetails);
            throw new IllegalStateException("User client is blocked");
        }
        if(!(userClientDetails.firstname().equals(firstname) || userClientDetails.lastname().equals(lastname) || userClientDetails.email().equals(email))) {

            logger.info("User information is don't match with existing user, input data:  email:{}, firstname:{}, lastname:{}," +
                    " existing data: email:{}, firstname:{}, lastname:{}", email, firstname, lastname,
                    userClientDetails.email(), userClientDetails.firstname(), userClientDetails.lastname());

            throw new IllegalStateException("User information is don't match with existing user");
        }
        return new User(userClientDetails.username(), userClientDetails.email(),userClientDetails.firstname(), userClientDetails.lastname()
        , password,false,userClientDetails.enabled());
    }
}
