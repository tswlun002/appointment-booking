package lunga.appointmentbooking.user.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.stream.IntStream;
import static lunga.appointmentbooking.user.domain.UsernameGenerator.username_ALLOWED_CONSECUTIVE_IDENTICAL_DIGITS;
import static org.assertj.core.api.Assertions.assertThat;

class UsernameGeneratorTest {

    @Test
    public  void generateId() {
        UsernameGenerator  username = new UsernameGenerator();
        String  usernameString =String.valueOf( username.getId());
        var valid = usernameString.charAt(0) != 0
                &&  usernameString.length() == UsernameGenerator.username_LENGTH_MAX_VALUE;
        valid = IntStream.range(0,  usernameString.length() - username_ALLOWED_CONSECUTIVE_IDENTICAL_DIGITS).anyMatch(i ->
                !IntStream.range(0, username_ALLOWED_CONSECUTIVE_IDENTICAL_DIGITS+i).allMatch(idDigit ->  usernameString.indexOf(idDigit) ==  usernameString.charAt(0))

        );

        assertThat(valid).isTrue();
    }
    @ParameterizedTest
    @ValueSource(strings = {"1233248000", "1233248800", "1263240808", "1233240855"})
    public  void generatedValidId(String username) {

        assertThat(UsernameGenerator.isValid(username)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1233200000", "1233333800", "1111140808", "1111111111","13240808", "123324085","1","78","987","3254","86543","9875210",
    "","TWwjwjh","oooooo","lllllll"})
    public  void generatedInValidId(String username) {

        assertThat(UsernameGenerator.isValid(username)).isFalse();
    }

}