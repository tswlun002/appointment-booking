package capitec.branch.appointment.location.infrastructure.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for Capitec Branch Locator API.
 * Maps to: https://www.capitecbank.co.za/api/Branch
 */
record CapitecBranchApiResponse(
        @JsonProperty("Branches")
        List<CapitecBranchDto> branches
) {
    record CapitecBranchDto(
            @JsonProperty("Id")
            String id,

            @JsonProperty("Code")
            String code,

            @JsonProperty("Latitude")
            Double latitude,

            @JsonProperty("Longitude")
            Double longitude,

            @JsonProperty("Name")
            String name,

            @JsonProperty("AddressLine1")
            String addressLine1,

            @JsonProperty("AddressLine2")
            String addressLine2,

            @JsonProperty("OperationHours")
            Map<DayTypeResponse,OperationTimeResponse> operationHours,

            @JsonProperty("City")
            String city,

            @JsonProperty("Province")
            String province,

            @JsonProperty("IsAtm")
            Boolean isAtm,

            @JsonProperty("CashAccepting")
            Boolean cashAccepting,

            @JsonProperty("HandlesHomeLoans")
            Boolean handlesHomeLoans,

            @JsonProperty("IsClosed")
            Boolean isClosed,

            @JsonProperty("BusinessBankCenter")
            Boolean businessBankCenter
    ) {
        boolean isActualBranch() {
            return !Boolean.TRUE.equals(isAtm) && code != null;
        }
    }
}

