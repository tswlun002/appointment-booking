package capitec.branch.appointment.location.infrastructure.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for Capitec Branch Locator API.
 * Maps to: https://www.capitecbank.co.za/api/Branch
 */
record CapitecBranchApiResponse(
        @JsonProperty("branches")
        List<CapitecBranchDto> branches
) {
    record CapitecBranchDto(
            @JsonProperty("id")
            String id,

            @JsonProperty("code")
            String code,

            @JsonProperty("latitude")
            Double latitude,

            @JsonProperty("longitude")
            Double longitude,

            @JsonProperty("name")
            String name,

            @JsonProperty("addressLine1")
            String addressLine1,

            @JsonProperty("addressline2")
            String addressLine2,

            @JsonProperty("operationhours")
            Map<DayTypeResponse,OperationTimeResponse> operationHours,

            @JsonProperty("city")
            String city,

            @JsonProperty("province")
            String province,

            @JsonProperty("isAtm")
            Boolean isAtm,

            @JsonProperty("cashAccepting")
            Boolean cashAccepting,

            @JsonProperty("handlesHomeLoans")
            Boolean handlesHomeLoans,

            @JsonProperty("IsClosed")
            Boolean isClosed,

            @JsonProperty("businessBankCenter")
            Boolean businessBankCenter
    ) {
        boolean isActualBranch() {
            return !Boolean.TRUE.equals(isAtm) && code != null;
        }
    }
}

