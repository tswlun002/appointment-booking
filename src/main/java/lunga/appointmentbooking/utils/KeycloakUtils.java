package lunga.appointmentbooking.utils;

import lunga.appointmentbooking.exeption.ApplicationHandler;
import lunga.appointmentbooking.exeption.EntityAlreadyExistException;
import lunga.appointmentbooking.exeption.UnAuthorizedException;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.HttpHostConnectException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Supplier;
@Slf4j
public class KeycloakUtils {
    public static   <T> T keyCloakRequest(Supplier<T> supplier,  @NotBlank String operation, Class<?> clazz) {
        try {
            return supplier.get();
        } catch (HttpClientErrorException e) {
            if(ApplicationHandler.instanceOfCustomerError(e)) {
                throw e;
            }
            // * BadRequestException  is thrown when then keycloak database is down
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("Keycloak service is down, connections failed.", e);
                throw new InternalServerErrorException("Sorry for inconvenience. Our service is down please try again later.");
            }
            log.error("Failed to {}, response:", operation, e);
            throw e;
        } catch (Exception e) {

            if(ApplicationHandler.instanceOfCustomerError(e)) {
              if(e instanceof ClientErrorException) clientErrorException(operation,(ClientErrorException)e, clazz);
              else throw e;
            }
            // * HttpHostConnectException is thrown when the keycloak is down
            else if (e.getCause() instanceof InternalServerErrorException || e instanceof BadRequestException ||
                    e.getCause() instanceof HttpHostConnectException || e.getCause() instanceof UnknownHostException) {

                //if(e.getCause() instanceof HttpHostConnectException || e.getCause() instanceof  org.apache.hc.client5.http.HttpHostConnectException){

                log.error("Keycloak service is down, connections failed.", e);
                throw new InternalServerErrorException("Sorry for inconvenience. Our service is down please try again later.");
            }

            log.error("Failed to {}\nUnexpect error ", operation, e);
            throw new InternalServerErrorException("Internal server error");
        }
    }
    public static  void clientErrorException(String operation,ClientErrorException clientErrorException,Class<?> entity ) {
        HttpStatus httpStatus = HttpStatus.valueOf(clientErrorException.getResponse().getStatus());
        switch (httpStatus){
            case  HttpStatus.CONFLICT->{
                log.error("{} already exists",entity, clientErrorException);
                throw new EntityAlreadyExistException(String.format("%s already exists", entity.getSimpleName()));
            }
            case  HttpStatus.NOT_FOUND->{

                AppDomains.fromValue(entity.getName()).ifPresent(v-> {
                            log.error("{} not found", entity, clientErrorException);
                            throw new NotFoundException(String.format("%s not found", entity.getSimpleName()));
                        }
                    );
                log.error("{} not found in keycloak, please double check necessary permissions to manage {}", entity, entity.getSimpleName(), clientErrorException);
                throw  new InternalServerErrorException("Internal server error");

            }
            case  HttpStatus.UNAUTHORIZED->{
                log.error("Client not authorized {}",operation);
                throw new UnAuthorizedException("Client unauthorized");
            }
            case  HttpStatus.BAD_REQUEST->{
                log.error("Invalid  user input, bad request {}",operation, clientErrorException);
                throw  new ResponseStatusException(HttpStatus.BAD_REQUEST, clientErrorException.getResponse().getStatusInfo().getReasonPhrase(), clientErrorException);
            }
        }
    }

    enum   AppDomains {

        User("lunga.appointmentbooking.user.domain.User"),
        Account("lunga.appointmentbooking.account.domain.Account"),
        Role("lunga.appointmentbooking.roles.domain.Role");

        private  String domain;
        AppDomains(String s) {
            this.domain = s;
        }
        public String getDomain() {
            return domain;
        }

        public  static Optional<AppDomains> fromValue(String domainPackage){
            for (AppDomains domain : AppDomains.values()) {
                if (domain.getDomain().equals(domainPackage)) {
                    return Optional.of(domain);
                }
            }
            log.warn("{} not found in our domains", domainPackage);
            return Optional.empty();
        }
    }
}
