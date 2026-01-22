package capitec.branch.appointment.location.infrastructure.api;

import com.fasterxml.jackson.annotation.JsonProperty;

 sealed interface CapitecBranchApiRequest {

    int DEFAULT_TAKE = 3000;

    record AreaSearchRequest(
            @JsonProperty("Query")
            String query,
            @JsonProperty("Take")
            int take
    ) implements CapitecBranchApiRequest {
        public AreaSearchRequest(String query) {
            this(query, DEFAULT_TAKE);
        }
    }


    record CoordinatesSearchRequest(
            @JsonProperty("Latitude")
            double latitude,
            @JsonProperty("Longitude")
            double longitude,
            @JsonProperty("Take")
            int take
    ) implements CapitecBranchApiRequest {
        public CoordinatesSearchRequest(double latitude, double longitude) {
            this(latitude, longitude, DEFAULT_TAKE);
        }
    }
}

