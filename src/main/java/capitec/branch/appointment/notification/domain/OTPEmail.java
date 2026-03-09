package capitec.branch.appointment.notification.domain;
import capitec.branch.appointment.utils.NamesValidator;
import capitec.branch.appointment.utils.NotBlankEmailValidator;
import capitec.branch.appointment.utils.OTPCodeValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

public record OTPEmail(
        String email,
        String fullname,
        String OTPCode,
        String traceId,
        Notification.UserEventType eventType
)  implements Email<Notification.UserEventType>
{
    public OTPEmail {

        Assert.isTrue(NotBlankEmailValidator.isValid(email), "Email is required");

        Assert.isTrue(NamesValidator.isValid(fullname), "Fullname is required");

        Assert.isTrue(OTPCodeValidator.isValid(OTPCode), "Valid OTP Code is required");

        Assert.isTrue(StringUtils.isNotBlank(traceId), "TraceId is required");

        Assert.notNull(eventType, "EventType is required");
    }
}
