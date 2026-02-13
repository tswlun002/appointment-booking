package capitec.branch.appointment.event.infrastructure.adapter.otp;

import capitec.branch.appointment.event.app.port.OTPPort;
import capitec.branch.appointment.otp.app.CreateOTPUseCase;
import capitec.branch.appointment.otp.app.CreateOtpDto;
import capitec.branch.appointment.otp.app.DeleteUserOTPsUseCase;
import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.otp.domain.OTPPurpose;
import capitec.branch.appointment.otp.domain.OTPStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OTPAdapter implements OTPPort {

    private final OTPService otpService;
    private final CreateOTPUseCase createOTPUseCase;
    private final DeleteUserOTPsUseCase deleteUserOTPsUseCase;


    @Override
    public void verifyOTP(String otpValue, String username) {
        otpService.find(username, otpValue, OTPStatus.VALIDATED)
                .ifPresent(otp -> {
                    otpService.update(otp,OTPStatus.VALIDATED);

                });
    }

    @Override
    public String generateOTP(String username, String traceId, String purpose) {
        return  createOTPUseCase.execute(new CreateOtpDto(username, traceId, OTPPurpose.valueOf(purpose))).getCode();
    }

    @Override
    public void deleteOTP(String username, String traceId) {
        deleteUserOTPsUseCase.execute(username, traceId);
    }
}
