package capitec.branch.appointment.user.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.user.app.dto.NewUserDtO;
import capitec.branch.appointment.user.app.event.UserCreatedEvent;
import capitec.branch.appointment.user.domain.*;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class RegisterUserUseCase {

    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ClientDomain clientDomain;

    public User execute(@Valid NewUserDtO registerDTO, String traceId) {

        Assert.isTrue(registerDTO.confirmPassword().equals(registerDTO.password()), "Passwords don't match");
        User user;

        if (registerDTO.isCapitecClient()) {

            var createUserExistingClientFactory = new CreateUserExistingClientFactory(
                    registerDTO.firstname(),
                    registerDTO.lastname(),
                    registerDTO.email(),
                    registerDTO.password()
            );

            user = createUserExistingClientFactory.createUser(() ->
                    clientDomain.findByUsername(registerDTO.idNumber()).orElseThrow(() -> {
                        log.error("User is not found with idNumber {}, traceId:{}", registerDTO.idNumber(), traceId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist as a capitec client");
                    }));

        } else {

            clientDomain.findByUsername(registerDTO.idNumber()).ifPresent(presentUser -> {
                log.error("UnAuthorized to use other client data. Client try to register as non-capitec client but existing client with existing data {}, traceId:{}", registerDTO.idNumber(), traceId);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UnAuthorized to use other client data");
            });

            do {
                user = new User(registerDTO.email(), registerDTO.firstname(), registerDTO.lastname(), registerDTO.password());
            } while (userService.checkIfUserExists(user.getUsername()));
        }

        // Check if email is taken
        userService.getUserByEmail(user.getEmail()).ifPresent(u -> {
            log.error("User already exists with email:{}, traceId:{}", u.getEmail(), traceId);
            throw new EntityAlreadyExistException("User already exists with email");
        });

        user = userService.registerUser(user);

        if (user == null) {
            log.error("Failed to register new user, traceId:{}", traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to register new user");
        }

        log.info("Registered user, traceId:{}", traceId);
        applicationEventPublisher.publishEvent(new UserCreatedEvent(
                user.getUsername(),
                user.getEmail(),
                user.getFirstname() + " " + user.getLastname(),
                traceId
        ));

        return user;
    }
}
