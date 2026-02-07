package capitec.branch.appointment.appointment.infrastructure.adapter;

import capitec.branch.appointment.appointment.app.port.BranchInfoPort;
import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.location.domain.BranchLocationFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BranchInfoAdapter implements BranchInfoPort {

    private static final String SOUTH_AFRICA = "South Africa";
    private final BranchLocationFetcher branchLocationFetcher;

    @Override
    public Optional<BranchInfo> getBranchInfo(String branchId) {
        try {
            return branchLocationFetcher.fetchByArea(SOUTH_AFRICA).stream()
                    .filter(branch -> branchId.equals(branch.getBranchCode()))
                    .findFirst()
                    .map(this::toBranchInfo);
        } catch (Exception e) {
            log.warn("Failed to fetch branch info for branchId: {}", branchId, e);
            return Optional.empty();
        }
    }

    private BranchInfo toBranchInfo(BranchLocation branch) {
        return new BranchInfo(
                branch.getBranchCode(),
                branch.getName(),
                branch.getAddress().getFullAddress()
        );
    }
}
