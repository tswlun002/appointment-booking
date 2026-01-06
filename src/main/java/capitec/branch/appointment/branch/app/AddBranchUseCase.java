package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class AddBranchUseCase {

    private final BranchService branchService;

    public Branch execute(@Valid BranchDTO branchInput) {

        if (branchInput == null) {
            log.error("Branch input is null");
            throw new IllegalArgumentException("Branch input is null, it cannot be added.");
        }

        Branch branch = new Branch(branchInput.branchId(), branchInput.openTime(), branchInput.closingTime(), branchInput.address());

       try {
           branch = branchService.add(branch);
       } catch (Exception e) {
           log.warn("Unable to add branch with ID: {}", branchInput.branchId(), e);
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error during branch creation");
       }
       return branch;
    }
}