package capitec.branch.appointment.exeption;

import lombok.Builder;

@Builder
public record AppException(
        String statusCodeMessage,
        String message,
        String path,
        String timestamp,
        Integer status

        ) {

}