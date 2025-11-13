package lunga.appointmentbooking.user.domain;


import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import lunga.appointmentbooking.utils.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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
     * @param field  is the invalid(cause execution to throw error) field and tested
     */
    @ParameterizedTest
    @CsvSource(value = {
            /**
             * Tests Invalid email
             * 1. Empty/null email
             * 2. Not include @ character
             */
            ";201_tgsw_T;Lunga;Tsewu;T(lunga.appointmentbooking.utils.Validator);EMAIL_MESS",
            "1998iungatsewugmail.com;B6fa51fe-d946-4a25-a01e-dce4f7b22efd;Lunga;Tsewu;T(lunga.appointmentbooking.utils.Validator);EMAIL_MESS",

            /**
             * Tests Invalid firstname/name
             ** 1. Empty lastname
             *  2. one letter
             */
            "tsw@gmail.com;4e7749d9-5e57-44ce-b01e-f8688730e5F;;Tsewu;T(lunga.appointmentbooking.utils.Validator);NAMES_FIELDS.apply('firstname',2)",
            "tsw@gmail.com;4e7749d9-5e57-44ce-b01e-f8688730e5fF;L;Tsewu;T(lunga.appointmentbooking.utils.Validator);NAMES_FIELDS.apply('firstname',2)",
            /**
             * Tests for invalid lastname/surname
             * 1. Empty lastname
             * 2. one letter
             */
            "tsw@gmail.com;4e7749d9-5e57-44ce-b01e-f8688730e5fF;Lu;;T(lunga.appointmentbooking.utils.Validator);NAMES_FIELDS.apply('lastname',2)",
            "tsw@gmail.com;4e7749d9-5e57-44ce-b01e-f8688730e5fF;Lu;t;T(lunga.appointmentbooking.utils.Validator);NAMES_FIELDS.apply('lastname',2)",
            /**
             * Tests for invalid lastname/surname
             * 1. Empty lastname
             * 2. one letter
             */
            "tsw@gmail.com;;Lu;tsew;T(lunga.appointmentbooking.utils.Validator);PASSWORD_MESS",
            "tsw@gmail.com;t;Lu;tsew;T(lunga.appointmentbooking.utils.Validator);PASSWORD_MESS",
            "tsw@gmail.com;1232324345;Lu;Vulputatelorem;T(lunga.appointmentbooking.utils.Validator);PASSWORD_MESS",
            "tsw@gmail.com;TSWWGWWL;Lu;LouisYao;T(lunga.appointmentbooking.utils.Validator);PASSWORD_MESS",
            "tsw@gmail.com;tstwwkwhsgs;Lu;GangWei;T(lunga.appointmentbooking.utils.Validator);PASSWORD_MESS",
            "tsw@gmail.com;@@$@$#^@^$@;Lu;KamalRuiz;T(lunga.appointmentbooking.utils.Validator);PASSWORD_MESS",
    }, delimiter = ';')
    void testInValidUserCreation(String email, String password, String name,String surname, String validatorClass,String field) {

        var mess = new SpelExpressionParser().parseExpression(validatorClass+"."+field).getValue(String.class);
        assertThatException().isThrownBy(() -> new User(email, name, surname, password))
                .isInstanceOf(ConstraintViolationException.class)
                .withMessage(mess);
    }

    /**
     * Test invalid field at time
     * @param fields  is the invalid(cause execution to throw error) field and tested
     */
    @ParameterizedTest
    @MethodSource("AllInvalidFields")
    void testAllInValidFieldAtSameTimeUserCreation(String email, String password, String name,String surname, String validatorClass,List<String> fields) {

        var messages = fields.stream().map(f->new SpelExpressionParser().parseExpression(validatorClass+"."+f).getValue(String.class)).
                sorted(Comparator.comparingInt(s -> s != null ? s.length() : 0)).collect(Collectors.joining("; "));

        assertThatException().isThrownBy(() -> new User(email, name, surname, password))
                .isInstanceOf(ConstraintViolationException.class)
                .withMessage(messages);
    }
    private Stream<Arguments> AllInvalidFields(){
        return Stream.of(Arguments.of("1998iungatsewugmail.com","@@$@$#^@^$@",null,"l","T(lunga.appointmentbooking.utils.Validator)",
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
                .isInstanceOf(ConstraintViolationException.class)
                .withMessage(Validator.PASSWORD_MESS);
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

        var errorMess = new SpelExpressionParser().parseExpression("T(lunga.appointmentbooking.utils.Validator).NAMES_FIELDS.apply('firstname',2)")
                .getValue(String.class);
        assertThatException().isThrownBy(()->   new User("hdsadgsag@gmail.com",firstname,"SoniaGuerrero","2Dd745f4-f113-482e-81b8-58f2fa7d5e12"))
                .isInstanceOf(ConstraintViolationException.class)
                .withMessage(errorMess);
    }

    @ParameterizedTest
    @CsvSource(value={"983","5872d538-1a0e-4b7f-a580-2b451475996",":"})
    void testSetInValidLastname(String lastname) {
        var errorMess = new SpelExpressionParser().parseExpression("T(lunga.appointmentbooking.utils.Validator).NAMES_FIELDS.apply('lastname',2)")
                .getValue(String.class);

        assertThatException().isThrownBy(()->new User("dgagdsfh@yahoo.com", "UrmilaPatel", lastname, "8cC5cf46-937f-44b3-83ed-f4469d5e2102" ))
                .isInstanceOf(ConstraintViolationException.class)
                .withMessage(errorMess);
    }


}