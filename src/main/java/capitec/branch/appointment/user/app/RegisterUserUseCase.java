package capitec.branch.appointment.user.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.user.app.dto.NewUserDtO;
import capitec.branch.appointment.user.app.event.UserCreatedEvent;
import capitec.branch.appointment.user.app.port.CapitecClientPort;
import capitec.branch.appointment.user.app.port.UserPersistencePort;
import capitec.branch.appointment.user.app.port.UserQueryPort;
import capitec.branch.appointment.user.domain.CreateUserExistingClientFactory;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.user.domain.UserDomainException;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class RegisterUserUseCase {

    private static final int MAX_USERNAME_GENERATION_ATTEMPTS = 3;

    private final UserPersistencePort userPersistencePort;
    private final UserQueryPort userQueryPort;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final CapitecClientPort capitecClientPort;

    @Transactional
    public User execute(@Valid NewUserDtO registerDTO, String traceId) {
        try {
            Assert.hasText(traceId, "traceId must not be blank");
            validatePasswordsMatch(registerDTO);

            User user = createUser(registerDTO, traceId);
            validateEmailNotTaken(user.getEmail(), traceId);

            user = persistUser(user, traceId);
            publishUserCreatedEvent(user, traceId);

            return user;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. traceId: {}, error: {}", traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (UserDomainException e) {
            log.error("User domain error. traceId: {}, error: {}", traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    private void validatePasswordsMatch(NewUserDtO registerDTO) {
        Assert.isTrue(
                registerDTO.confirmPassword().equals(registerDTO.password()),
                "Passwords don't match"
        );
    }

    private User createUser(NewUserDtO registerDTO, String traceId) {
        if (registerDTO.isCapitecClient()) {
            return createCapitecClientUser(registerDTO, traceId);
        }
        return createGuestUser(registerDTO, traceId);
    }

    private User createCapitecClientUser(NewUserDtO registerDTO, String traceId) {
        var factory = new CreateUserExistingClientFactory(
                registerDTO.email(),
                registerDTO.firstname(),
                registerDTO.lastname(),
                registerDTO.password()
        );

        return factory.createUser(() ->
                capitecClientPort.findByIdNumber(registerDTO.idNumber())
                        .orElseThrow(() -> {
                            log.error("Capitec client not found with idNumber: {}, traceId: {}", registerDTO.idNumber(), traceId);
                            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist as a Capitec client");
                        })
        );
    }

    private User createGuestUser(NewUserDtO registerDTO, String traceId) {
        validateNotCapitecClient(registerDTO.idNumber(), traceId);
        return generateUserWithUniqueUsername(registerDTO);
    }

    private void validateNotCapitecClient(String idNumber, String traceId) {
        capitecClientPort.findByIdNumber(idNumber).ifPresent(__ -> {
            log.error("Registration conflict: ID number belongs to existing Capitec client. idNumber: {}, traceId: {}", idNumber, traceId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This ID number is already registered as a Capitec client. Please register as an existing client.");
        });
    }

    private User generateUserWithUniqueUsername(NewUserDtO registerDTO) {
        for (int attempt = 0; attempt < MAX_USERNAME_GENERATION_ATTEMPTS; attempt++) {
            User user = new User(
                    registerDTO.email(),
                    registerDTO.firstname(),
                    registerDTO.lastname(),
                    registerDTO.password()
            );

            if (!userQueryPort.checkIfUserExists(user.getUsername())) {
                return user;
            }
        }

        log.error("Failed to generate unique username after {} attempts for email: {}", MAX_USERNAME_GENERATION_ATTEMPTS, registerDTO.email());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate unique username. Please try again.");
    }

    private void validateEmailNotTaken(String email, String traceId) {
        userQueryPort.getUserByEmail(email).ifPresent(__ -> {
            log.error("Registration failed: User with email already exists, traceId: {}", traceId);
            throw new EntityAlreadyExistException("User already exists with this email address");
        });
    }

    private User persistUser(User user, String traceId) {
        User registeredUser = userPersistencePort.registerUser(user);

        if (registeredUser == null) {
            log.error("Failed to persist user to identity provider, traceId: {}", traceId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to register user. Please try again.");
        }

        log.info("User registered successfully. username: {}, traceId: {}", registeredUser.getUsername(), traceId);
        return registeredUser;
    }

    private void publishUserCreatedEvent(User user, String traceId) {
        String fullName = user.getFirstname() + " " + user.getLastname();
        applicationEventPublisher.publishEvent(new UserCreatedEvent(
                user.getUsername(),
                user.getEmail(),
                fullName,
                traceId
        ));
        log.debug("UserCreatedEvent published for username: {}, traceId: {}", user.getUsername(), traceId);
    }
}
