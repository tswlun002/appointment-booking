package capitec.branch.appointment.location.infrastructure.api;

import com.fasterxml.jackson.annotation.JsonProperty;

 sealed interface CapitecBranchApiRequest {

    int DEFAULT_TAKE = 50;

    record AreaSearchRequest(
            @JsonProperty("query")
            String query,
            @JsonProperty("take")
            int take
    ) implements CapitecBranchApiRequest {
        public AreaSearchRequest(String query) {
            this(query, DEFAULT_TAKE);
        }
    }


    record CoordinatesSearchRequest(
            @JsonProperty("latitude")
            double latitude,
            @JsonProperty("longitude")
            double longitude,
            @JsonProperty("take")
            int take
    ) implements CapitecBranchApiRequest {
        public CoordinatesSearchRequest(double latitude, double longitude) {
            this(latitude, longitude, DEFAULT_TAKE);
        }
    }
}

