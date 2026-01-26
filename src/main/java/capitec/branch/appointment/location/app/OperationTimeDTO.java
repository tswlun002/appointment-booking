package capitec.branch.appointment.location.app;

import java.time.LocalTime;

public record OperationTimeDTO(
        LocalTime openAt,
        LocalTime closeAt,
        boolean closed,
        boolean isHoliday
){

}