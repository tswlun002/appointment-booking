package capitec.branch.appointment.location.infrastructure.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;

public record OperationTimeResponse(
        @JsonProperty("openAt")
        LocalTime openAt,
        @JsonProperty("closeAt")
        LocalTime closeAt,
        @JsonProperty(value = "closed")
        boolean closed
) {
}
