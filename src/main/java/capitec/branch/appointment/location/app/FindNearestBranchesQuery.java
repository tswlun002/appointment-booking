package capitec.branch.appointment.location.app;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FindNearestBranchesQuery(
        @NotNull(message = "Latitude is required")
        @Min(value = -90, message = "Latitude must be between -90 and 90")
        @Max(value = 90, message = "Latitude must be between -90 and 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @Min(value = -180, message = "Longitude must be between -180 and 180")
        @Max(value = 180, message = "Longitude must be between -180 and 180")
        Double longitude,

        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 50, message = "Limit cannot exceed 50")
        Integer limit,

        @Min(value = 1, message = "Max distance must be at least 1 km")
        @Max(value = 500, message = "Max distance cannot exceed 500 km")
        Double maxDistanceKm
) {
    public FindNearestBranchesQuery {
        if (limit == null) {
            limit = 10;
        }
    }
}

