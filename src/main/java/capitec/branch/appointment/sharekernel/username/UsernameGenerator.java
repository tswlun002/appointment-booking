package capitec.branch.appointment.sharekernel.username;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
public class UsernameGenerator {
    public static final int username_LENGTH_MAX_VALUE = 10;
    public static final int username_LENGTH_MIN_VALUE = 0;
    public static final int username_ALLOWED_CONSECUTIVE_IDENTICAL_DIGITS = 3;
    private static final  String REGEX = "[^0-9]";
    private static final  String REGEX_VALIDATOR = "^[1-9]\\d{9}$";

    private long id;

    public UsernameGenerator() {
        do {
            this.id = generateIdFromUUID();
        } while (hasTooManyConsecutiveDigits(this.id));

    }

    public   String getId(){
        return String.valueOf(id);
    }

    public static boolean isValid(String username) {
        boolean matches = StringUtils.isNotBlank(username) && username.matches(REGEX_VALIDATOR);
        return  matches &&
                (!hasTooManyConsecutiveDigits(Long.parseLong(username)));

    }

    private Long generateIdFromUUID() {
        String uuid = UUID.randomUUID().toString().replaceAll(REGEX, ""); // Extract only numbers
        String idStr = uuid.replaceAll("^0+", ""); // Remove leading zeros

        // Ensure exactly username_LENGTH_MAX_VALUE digits
        if (idStr.length() < username_LENGTH_MAX_VALUE) {
            idStr += UUID.randomUUID().toString().replaceAll(REGEX, "").substring(username_LENGTH_MIN_VALUE
                    , username_LENGTH_MAX_VALUE - idStr.length());
        }

        return Long.parseLong(idStr.substring(username_LENGTH_MIN_VALUE, username_LENGTH_MAX_VALUE));
    }

    private static boolean hasTooManyConsecutiveDigits(Long number) {
        String str = String.valueOf(number);
       return IntStream.range(0,username_LENGTH_MAX_VALUE - username_ALLOWED_CONSECUTIVE_IDENTICAL_DIGITS).anyMatch(i->
           IntStream.range(i, username_ALLOWED_CONSECUTIVE_IDENTICAL_DIGITS+i).allMatch(idDigit -> str.charAt(idDigit) == str.charAt(i))
        );
    }

}
