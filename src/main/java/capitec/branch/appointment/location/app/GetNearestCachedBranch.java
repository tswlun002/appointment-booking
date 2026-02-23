package capitec.branch.appointment.location.app;

import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.location.domain.Coordinates;

import java.util.List;

public interface GetNearestCachedBranch {

    List<BranchLocation> findNearByBranches(Coordinates customerLocation, double nearbyRadiusKM);
}
