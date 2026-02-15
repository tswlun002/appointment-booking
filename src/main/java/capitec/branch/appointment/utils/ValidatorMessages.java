package capitec.branch.appointment.utils;


import capitec.branch.appointment.role.domain.Role;
import capitec.branch.appointment.sharekernel.username.UsernameGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Component
@Slf4j
public class ValidatorMessages {

    public static final String ROLE_TYPE_MESSAGE = "Role type must be <channel name>_<group name> ";
    public static final String ROLE_TYPE_NAME_REGEX = "^[a-zA-Z]{2,}$";
    public static final String EVENT_TRACE_ID_MESS = "Trace eventId is required";
    public static  String PASSWORD_REGEX = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[=+`~_#?!@$%^&*-])[A-Za-z\\d=+`~_#?!@$%^&*-]{8,}$";
    public static final String ROLE_NAME_FORMAT = "ChanelName_Function_Entity ";
    public static final String ROLE_NAME_REGEX = "[a-zA-Z]+_[a-zA-Z]+";
    public static final String ROLE_NAME_MESSAGE = "Valid role must be "+ Role.MIN_ROLE_NAME_LENGTH+" characters long. Format: "+ ValidatorMessages.ROLE_NAME_FORMAT;
    public static final String USERNAME_MESSAGE = "Username must be "+ UsernameGenerator.username_LENGTH_MAX_VALUE+
            " digit, cannot start with zero and cannot have "+ UsernameGenerator.username_ALLOWED_CONSECUTIVE_IDENTICAL_DIGITS+" consecutive identical digits";
     // USER MESS
    public static final String EMAIL_MESS=   "Email  must be valid email address.";
    public static final String PASSWORD_MESS=   "Password must be 6+ character with special character,lower case,uppercase and special character";
    public static final String FIRSTNAME=   "Firstname must be at least 2 letter";
    public static final String LASTNAME=   "Lastname must be at least 2 letter";

    public static final String DATETIME_FORMATTER="yyyy-MM-dd'T'HH:mm:ss";


}
