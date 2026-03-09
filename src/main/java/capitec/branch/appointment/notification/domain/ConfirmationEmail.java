package capitec.branch.appointment.notification.domain;


import capitec.branch.appointment.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

public record ConfirmationEmail(
        String email,
        String fullname,
        String traceId,
        LocalDateTime createdAt,
        Notification.UserEventType eventType)  implements Email<Notification.UserEventType>{

    public  ConfirmationEmail{
        Assert.isTrue(NotBlankEmailValidator.isValid(email), "Email is required");

        Assert.isTrue(NamesValidator.isValid(fullname), "Fullname is required");

        Assert.isTrue(StringUtils.isNotBlank(traceId), "TraceId is required");

        Assert.notNull(eventType, "EventType is required");

        Assert.notNull(createdAt, "CreatedAt is required");

    }
}
