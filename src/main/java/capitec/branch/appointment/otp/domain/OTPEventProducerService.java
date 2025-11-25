package capitec.branch.appointment.otp.domain;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import capitec.branch.appointment.utils.OTPCode;
import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Validator;

import java.util.concurrent.CompletableFuture;

public interface OTPEventProducerService {
    CompletableFuture<Boolean> sendRegistrationEvent(@Username String Username, @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS)  String email,
                                                     @NotBlank(message = Validator.FIRSTNAME+" "+Validator.LASTNAME)
                                   String fullname,
                                                     @OTPCode String otpCode,
                                                     @NotBlank(message = Validator.EVENT_TRACE_ID_MESS)String traceId);
    CompletableFuture<Boolean>  sendPasswordResetRequestEvent( @Username String Username,@NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS)  String email,
                                          @NotBlank(message = Validator.FIRSTNAME+" "+Validator.LASTNAME)
                                          String fullname,
                                          @OTPCode String OTP,
                                          @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId);

    CompletableFuture<Boolean>  deleteUserRequestEvent( @Username String Username, @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS)  String email,
                                   @NotBlank(message = Validator.FIRSTNAME+" "+Validator.LASTNAME) String fullname,
                                   @OTPCode String OTP,
                                   @NotBlank(message = Validator.EVENT_TRACE_ID_MESS) String traceId);


}
