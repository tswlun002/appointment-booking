package capitec.branch.appointment.sharekernel.event.metadata;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.util.Assert;

import java.io.Serializable;


public  record OTPMetadata(

        @NotBlank(message = "Event name is required")
        String fullname,
        @NotBlank(message = "username is required")
        String username,
        @NotBlank(message = "Email is required")
        @Email
        String email,
        @NotBlank(message = "OtpCode cannot be null")
        String otpCode
) implements MetaData, Serializable {

    public  OTPMetadata {
        Assert.hasText(fullname, "Fullname is required");
        Assert.hasText(username, "Fullname is required");
        Assert.hasText(email, "Email is required");
        Assert.hasText(otpCode, "OtpCode is required");
    }



}
