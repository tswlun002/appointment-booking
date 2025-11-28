package capitec.branch.appointment.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import capitec.branch.appointment.utils.Name;
import capitec.branch.appointment.utils.Password;
import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Validator;
import org.apache.james.mime4j.dom.datetime.DateTime;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import static capitec.branch.appointment.utils.Validator.IS_USER_ENABLE_MESS;


@Slf4j
public class User {

    public static final int NAMES_FIELD_LENGTH = 2;
    @Username
    private final String username;
    @Email(message = Validator.EMAIL_MESS)
    @NotBlank(message = Validator.EMAIL_MESS)
    private final String email;
    @Name
    private String firstname;
    @Name
    private String lastname;
    @Password
    private String password;
    private Boolean verified;
    private Boolean enabled;
    private DateTime createdAt;

    public User(String email, String firstname, String lastname, String password) {
        log.debug("Create user :{}", email);
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
        this.verified = false;
        this.enabled = true;
        this.username = new UsernameGenerator().getId();
        log.debug("User:{} created successfully", this);
        validateUser();

    }

    @Username
    public String getUsername() {
        return username;
    }

    public final void validateUser() {

        BiFunction<String, String, ConstraintViolation<UsernameGenerator>> constraintViolationFunction = (field, errorMess) ->
                ConstraintViolationImpl.forBeanValidation(Validator.USERNAME_MESSAGE,
                        Collections.singletonMap(field, errorMess),
                        Map.of(), errorMess, UsernameGenerator.class, null, null, null, null,
                        null, null
                );

        if (!this.enabled) {

            Set<ConstraintViolation<UsernameGenerator>> isEnabled1 = Collections.singleton(constraintViolationFunction.apply("isEnabled", IS_USER_ENABLE_MESS));
            throw new ConstraintViolationException(IS_USER_ENABLE_MESS, isEnabled1);
        }
        Validator.validate(this);
        log.debug("User:{} validated successfully", this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.enabled = isEnabled;
        validateUser();
    }

    public void setVerified(boolean isVerified) {
        this.verified = isVerified;
        validateUser();
    }

    public boolean isVerified() {
        return verified;
    }

    @Email
    public String getEmail() {
        return email;
    }

    @Name
    public String getFirstname() {
        return firstname;
    }

    protected final User setFirstname(@Name String firstname) {
        this.firstname = firstname;
        validateUser();
        return this;
    }

    @Name
    public String getLastname() {
        return lastname;
    }

    protected final User setLastname(@Name String lastname) {
        this.lastname = lastname;
        validateUser();
        return this;
    }

    public final User setPassword(@Password String password) {
        this.password = password;
        validateUser();
        return this;
    }

    @JsonIgnoreProperties
    @Password
    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return username == user.username && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "username=" + username +
                ", email='" + email + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", password='" + password + '\'' +
                ", isVerified=" + verified +
                ", isEnabled=" + enabled +
                '}';
    }
}
