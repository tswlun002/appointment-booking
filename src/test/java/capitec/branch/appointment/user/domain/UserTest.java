package capitec.branch.appointment.user.domain;


import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import capitec.branch.appointment.utils.ValidatorMessages;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTest {

    @ParameterizedTest
    @CsvSource(value = {"19tawu@gmail.com:Lung:XingPerez:27c4e7ef-2e2a-4eb3-a62c-26e4e67f786E",
            "1998iungatsewu@yahoo.com:EmmaJackson:ChenIvanova:B6fa51fe-d946-4a25-a01e-dce4f7b22efd"},delimiter = ':')
    void CreateValidUser(String email, String firstname,String lastname, String password) {
        User user = new User(email, firstname, lastname, password);
        String output = user.getEmail();
        assertThat(output).isNotBlank().isEqualTo(email);
        assertThat(user.getPassword()).isNotNull().isEqualTo(password);
    }


    /**
     * Test invalid field at time
     * @param fields  is the invalid(cause execution to throw error) field and tested
     */
    @ParameterizedTest
    @MethodSource("AllInvalidFields")
    void testAllInValidFieldAtSameTimeUserCreation(String email, String password, String name,String surname, String validatorClass,List<String> fields) {

        assertThatException().isThrownBy(() -> new User(email, name, surname, password))
                .isInstanceOf(IllegalArgumentException.class);
    }
    private Stream<Arguments> AllInvalidFields(){
        return Stream.of(Arguments.of("1998iungatsewugmail.com","@@$@$#^@^$@",null,"l","T(capitec.branch.appointment.utils.Validator)",
                List.of("EMAIL_MESS","NAMES_FIELDS.apply('firstname',2)","NAMES_FIELDS.apply('lastname',2)","PASSWORD_MESS")));
    }



    @ParameterizedTest
    @CsvSource(value = {"@Vswuny0010","Lul+thatha1"})
    void setValidUserPassword(String password) {
        User user = new User("2004lu@yahoo.com", "MaksimBala", "KunRocha", password);
        assertThat(user.getPassword()).
                isNotNull()
                .isEqualTo(password);
        assertThat(user).isInstanceOf(User.class);
        assertThat(user.getEmail()).isNotNull().isEqualTo("2004lu@yahoo.com");
        assertThat(user.getFirstname()).isNotNull().isEqualTo("MaksimBala");
        assertThat(user.getLastname()).isNotNull().isEqualTo("KunRocha");

    }

    @ParameterizedTest
    @CsvSource(value = {"123456","b6fa51fe-d946-4a25-a01e-dce4f7b22efd"})
    void setInvalidUserPassword(String password) {
        assertThatException().isThrownBy(()-> new User("2004lu@yahoo.com", "MaksimBala", "KunRocha", password))
                .isInstanceOf(IllegalArgumentException.class)
                .withMessage(ValidatorMessages.PASSWORD_MESS);
    }

    @ParameterizedTest
    @CsvSource(value={"AlanHossain:NurSarkar","VictorHernandez:DeborahPham","ElizabethPerez:XiangSong"}, delimiter = ':')
    void testSetValidName(String firstname, String lastname) {
        User user = new User("2004lu@yahoo.com", firstname, lastname, "04fb3853-5816-4790-9350-Af364a5a7c1e");

        assertThat(user.getFirstname()).isEqualTo(firstname);
        assertThat(user.getLastname()).isEqualTo(lastname);
    }
    @ParameterizedTest
    @CsvSource(value={"@AlanHossain","5872d538-1a0e-4b7f-a580-2b45147599c6",":"})
    void testSetInValidFirstname(String firstname) {

      
        assertThatException().isThrownBy(()->   new User("hdsadgsag@gmail.com",firstname,"SoniaGuerrero","2Dd745f4-f113-482e-81b8-58f2fa7d5e12"))
                .isInstanceOf(IllegalArgumentException.class)
                .withMessage("Firstname must be at least 2 letter");
    }

    @ParameterizedTest
    @CsvSource(value={"983","5872d538-1a0e-4b7f-a580-2b451475996",":"})
    void testSetInValidLastname(String lastname) {


        assertThatException().isThrownBy(()->new User("dgagdsfh@yahoo.com", "UrmilaPatel", lastname, "8cC5cf46-937f-44b3-83ed-f4469d5e2102" ))
                .isInstanceOf(IllegalArgumentException.class)
                .withMessage("Lastname must be at least 2 letter");
    }


}