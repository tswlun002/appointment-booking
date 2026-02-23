package capitec.branch.appointment.event.infrastructure;

import jakarta.ws.rs.InternalServerErrorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;

@Slf4j
public record UserEventStatus(String  status) {

    public  UserEventStatus {

        if(StringUtils.isBlank(status) && ! DEAD_LETTER_STATUS.isValueOf(status)) {

            log.error("Event status is invalid, status is {}", status);
            throw new InternalServerErrorException("Internal Server Error");
        }
    }
}
