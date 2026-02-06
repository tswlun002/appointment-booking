package capitec.branch.appointment.location.app;

import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.location.domain.OperationTime;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

public record NearbyBranchDTO(
        String branchId,
        String branchCode,
        String name,
        String fullAddress,
        double latitude,
        double longitude,
        double distanceKm,
        Map<LocalDate, OperationTimeDTO> operationTimes,
        boolean businessBankCenter,
        boolean fromNearbyLocation
) {
    public static NearbyBranchDTO from(BranchLocation branch, double distanceKm) {
        return from(branch, distanceKm, false);
    }

    public static NearbyBranchDTO from(BranchLocation branch, double distanceKm, boolean fromNearbyLocation) {
        return new NearbyBranchDTO(
                branch.getBranchId(),
                branch.getBranchCode(),
                branch.getName(),
                branch.getAddress().getFullAddress(),
                branch.getCoordinates().latitude(),
                branch.getCoordinates().longitude(),
                Math.round(distanceKm * 100.0) / 100.0,
                branch.getDailyOperationTimes().values()
                        .stream()
                        .collect(Collectors.toMap(OperationTime::day,
                                operationTime ->
                                        new OperationTimeDTO(operationTime.openAt(),operationTime.closeAt(),operationTime.closed(),
                                                operationTime.isHoliday())
                        )),
                branch.isBusinessBankCenter(),
                fromNearbyLocation
        );
    }
}

