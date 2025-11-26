package capitec.branch.appointment.user.app;

import capitec.branch.appointment.utils.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NewUserDtO(
        String email,
        String password,
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
                     String password) {
        this(email, password,firstname,lastname, null,false);
    }

public   NewUserDtO(@NotBlank @Pattern(regexp = "[0-9]{0,13}")
                    String idNumber,
                    @NotNull
                    Boolean isCapitecClient) {
    this(null, null,null,null, idNumber,isCapitecClient);
}
}
