package capitec.branch.appointment.location.app;

import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.location.domain.OperationTime;

public record NearbyBranchDTO(
        String branchCode,
        String name,
        String fullAddress,
        double latitude,
        double longitude,
        double distanceKm,
        OperationTime weekdayHours,
        OperationTime saturdayHours,
        OperationTime sundayHours,
        boolean businessBankCenter,
        boolean fromNearbyLocation
) {
    public static NearbyBranchDTO from(BranchLocation branch, double distanceKm) {
        return from(branch, distanceKm, false);
    }

    public static NearbyBranchDTO from(BranchLocation branch, double distanceKm, boolean fromNearbyLocation) {
        return new NearbyBranchDTO(
                branch.getBranchCode(),
                branch.getName(),
                branch.getAddress().getFullAddress(),
                branch.getCoordinates().latitude(),
                branch.getCoordinates().longitude(),
                Math.round(distanceKm * 100.0) / 100.0,
                branch.getOperatingHours().weekdayHours(),
                branch.getOperatingHours().saturdayHours(),
                branch.getOperatingHours().sundayHours(),
                branch.isBusinessBankCenter(),
                fromNearbyLocation
        );
    }
}

