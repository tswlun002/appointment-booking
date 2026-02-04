package capitec.branch.appointment.location.infrastructure.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;

public record OperationTimeResponse(
        @JsonProperty("openAt")
        LocalTime openAt,
        @JsonProperty("closeAt")
        LocalTime closeAt,
        @JsonProperty(value = "closed")
        boolean closed,
        @JsonProperty("fromDay")
        String fromDay,
        @JsonProperty("toDay")
        String toDay
) {
}
//latitude=-33.960553&longitude=18.470156&limit=10&maxDistanceKm=25