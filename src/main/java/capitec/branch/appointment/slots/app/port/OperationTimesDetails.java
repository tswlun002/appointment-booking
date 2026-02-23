package capitec.branch.appointment.slots.app.port;


import java.time.LocalTime;

public record OperationTimesDetails(
        LocalTime openAt,
        LocalTime closeAt,
        boolean isClose

) {



}
