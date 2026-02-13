package capitec.branch.appointment.event.app.port;


import capitec.branch.appointment.utils.OTPCode;
import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.ValidatorMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.concurrent.CompletableFuture;

public interface OTPEventProducerServicePort {
    CompletableFuture<Boolean> sendRegistrationEvent(@Username String Username, @NotBlank(message = ValidatorMessages.EMAIL_MESS) @Email(message = ValidatorMessages.EMAIL_MESS)  String email,
                                                     @NotBlank(message = ValidatorMessages.FIRSTNAME+" "+ ValidatorMessages.LASTNAME)
                                   String fullname,
                                                     @OTPCode String otpCode,
                                                     @NotBlank(message = ValidatorMessages.EVENT_TRACE_ID_MESS)String traceId);
    CompletableFuture<Boolean>  sendPasswordResetRequestEvent( @Username String Username,@NotBlank(message = ValidatorMessages.EMAIL_MESS) @Email(message = ValidatorMessages.EMAIL_MESS)  String email,
                                          @NotBlank(message = ValidatorMessages.FIRSTNAME+" "+ ValidatorMessages.LASTNAME)
                                          String fullname,
                                          @OTPCode String OTP,
                                          @NotBlank(message = ValidatorMessages.EVENT_TRACE_ID_MESS) String traceId);

    CompletableFuture<Boolean>  deleteUserRequestEvent( @Username String Username, @NotBlank(message = ValidatorMessages.EMAIL_MESS) @Email(message = ValidatorMessages.EMAIL_MESS)  String email,
                                   @NotBlank(message = ValidatorMessages.FIRSTNAME+" "+ ValidatorMessages.LASTNAME) String fullname,
                                   @OTPCode String OTP,
                                   @NotBlank(message = ValidatorMessages.EVENT_TRACE_ID_MESS) String traceId);


}
