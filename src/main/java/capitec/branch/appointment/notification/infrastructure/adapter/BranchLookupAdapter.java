package capitec.branch.appointment.notification.infrastructure.adapter;

import capitec.branch.appointment.location.app.NearbyBranchDTO;
import capitec.branch.appointment.location.app.SearchBranchesByAreaQuery;
import capitec.branch.appointment.location.app.SearchBranchesByAreaUseCase;
import capitec.branch.appointment.notification.app.port.BranchDetails;
import capitec.branch.appointment.notification.app.port.BranchLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class BranchLookupAdapter implements BranchLookup {

    private final SearchBranchesByAreaUseCase searchBranchesByAreaUseCase;


    @Override
    public Optional<BranchDetails> findById(String branchId) {
        List<NearbyBranchDTO> branchDTOList = searchBranchesByAreaUseCase.execute(new SearchBranchesByAreaQuery("South Africa"));

       return branchDTOList.stream()
                .filter(branch->branch.branchId().equals(branchId))
                .findFirst()
                .map(branch->
                      new BranchDetails(branch.name(),branch.fullAddress())
                );

    }
}
