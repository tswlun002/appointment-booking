package capitec.branch.appointment.user.app.dto;

import capitec.branch.appointment.utils.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

 public   NewUserDtO(@Email
                     String email,
                     @NotBlank
                     @Pattern(regexp = "[a-zA-Z]{2,}")
                     String firstname,
                     @NotBlank
                     @Pattern(regexp = "[a-zA-Z]{2,}")
                     String lastname,
                     @Password
                     String password,
                     @Password
                     String confirmPassword) {
        this(email, password,confirmPassword ,firstname,lastname, null,false);
     Assert.isTrue(password.equals(confirmPassword),"Passwords do not match");
    }
}
