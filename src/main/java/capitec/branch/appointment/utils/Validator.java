package capitec.branch.appointment.utils;


import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.role.domain.Role;
import capitec.branch.appointment.user.domain.UsernameGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static capitec.branch.appointment.user.domain.User.NAMES_FIELD_LENGTH;
@Component
@Slf4j
public class Validator {

    public static final String ROLE_TYPE_MESSAGE = "Role type must be <channel name>_<group name> ";
    public static final String ROLE_TYPE_NAME_REGEX = "^[a-zA-Z]{2,}$";
    public static final String EVENT_TRACE_ID_MESS = "Trace eventId is required";
    public static  String PASSWORD_REGEX = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[=+`~_#?!@$%^&*-])[A-Za-z\\d=+`~_#?!@$%^&*-]{8,}$";
    public static final String ROLE_NAME_FORMAT = "ChanelName_Function_Entity ";
    public static final String ROLE_NAME_REGEX = "[a-zA-Z]+_[a-zA-Z]+";
    public static final String ROLE_NAME_MESSAGE = "Valid role must be "+ Role.MIN_ROLE_NAME_LENGTH+" characters long. Format: "+Validator.ROLE_NAME_FORMAT;
    public static final String USERNAME_MESSAGE = "Username must be "+ UsernameGenerator.username_LENGTH_MAX_VALUE+
            " digit, cannot start with zero and cannot have "+ UsernameGenerator.username_ALLOWED_CONSECUTIVE_IDENTICAL_DIGITS+" consecutive identical digits";
     // USER MESS
    public static final String EMAIL_MESS=   "Email  must be valid email address.";
    public static final String PASSWORD_MESS=   "Password must be 8+ character with special character,lower case,uppercase and special character";
    public static final String FIRSTNAME=   "Firstname must be at least 2 letter";
    public static final String LASTNAME=   "Lastname must be at least 2 letter";
    public static final BiFunction<String,Integer,String> NAMES_FIELDS =(field, minimumLength)-> String.format( "%s must be at least %d letters", field, minimumLength);
    public static final BiFunction<String,String,String> DEPENDENCY =(dependency,currentClass)-> String.format( "Dependency %s is required by %s", dependency,currentClass.toUpperCase());

    //OTP
    public  static final  String OTP_EXPIRE_TIME_MESS = "Expire time must be at least "+ OTP.EXPIRE_TIME_MIN+" minutes";

    // ACCOUNT MESS
    public static final String ACCOUNT_IMAGE_MESS="Image url must be valid";

    public static final String DATETIME_FORMATTER="yyyy-MM-dd'T'HH:mm:ss";


    public  static  <T> void validate(T a){

        if(a==null){

            log.error("Cannot validate null object, object.");
            throw new InternalServerErrorException("Cannot validate null object");
        }

        try(var factory= Validation.buildDefaultValidatorFactory()){

            Set<ConstraintViolation<T>> violations = factory.getValidator().validate(a);

            var errorMessages = String.join("; ", violations.stream().map(v -> {

                var propertyName = v.getPropertyPath().toString();
                var mess = FIELD_TYPE.fromFieldName(propertyName);

                if(mess==null) {
                    return v.getMessage();
                }

                return switch (mess) {
                    case EMAIL -> EMAIL_MESS;
                   case USERNAME -> USERNAME_MESSAGE;
                    case PASSWORD ->PASSWORD_MESS;
                    case NAME -> NAMES_FIELDS.apply(v.getPropertyPath().toString(), NAMES_FIELD_LENGTH);
                    case IMAGE_URL -> ACCOUNT_IMAGE_MESS;
                    case DEPENDENCY -> DEPENDENCY.apply(propertyName, a.getClass().toString());
                };
            }).sorted(Comparator.comparingInt(String::length)).collect(Collectors.toCollection(LinkedHashSet::new)));

            if (StringUtils.isNotBlank(errorMessages)) {

                log.error("Error was thrown message:{}\n error stack:{}",errorMessages, violations);

                throw new ConstraintViolationException(errorMessages, violations);
            }
        }
    }
}
