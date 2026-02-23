package capitec.branch.appointment.user.app.dto;

import capitec.branch.appointment.utils.*;
import org.springframework.util.Assert;

public record NewUserDtO(
        String email,
        String password,
        String confirmPassword,
        String firstname,
        String lastname,
        String idNumber,
        boolean isCapitecClient
) {

 public   NewUserDtO(@CustomerEmail
                     String email,
                     @Name(message = ValidatorMessages.FIRSTNAME)
                     String firstname,
                     @Name(message = ValidatorMessages.LASTNAME)
                     String lastname,
                     @Password
                     String password,
                     @Password
                     String confirmPassword) {
        this(email, password,confirmPassword ,firstname,lastname, null,false);
     Assert.isTrue(password.equals(confirmPassword),"Passwords do not match");
    }
}
