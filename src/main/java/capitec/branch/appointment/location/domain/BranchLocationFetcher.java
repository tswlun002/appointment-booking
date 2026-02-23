package capitec.branch.appointment.location.domain;

import java.util.List;

public interface BranchLocationFetcher {

    List<BranchLocation> fetchByCoordinates(Coordinates coordinates);

    List<BranchLocation> fetchByArea(String searchText);
}

