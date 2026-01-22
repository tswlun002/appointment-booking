package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.app.port.BranchOperationHoursPort;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.exeption.BranchIsClosedException;
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
    private final BranchOperationHoursPort branchOperationHoursPort;
    private final static String COUNTRY = "South Africa";

    public Branch execute(@Valid BranchDTO branchInput) {

       try {

           checkIfExists(branchInput.branchId());

           Branch branch = new Branch(branchInput.branchId());
           return branchService.add(branch);


       } catch (ResponseStatusException e) {
           throw e ;
       }
       catch (BranchIsClosedException e) {

           log.warn("Branch is closed, branch:{}",branchInput.branchId(), e);
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The branch is closed.",e);

       } catch (IllegalArgumentException | IllegalStateException e) {
           log.warn("Invalid branch input: {}", branchInput, e);
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(),e);
       }
       catch (Exception e) {
           log.warn("Unable to add branch with ID: {}", branchInput.branchId(), e);
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error during branch creation");
       }

    }

    private  void checkIfExists(String branchId){
        if (!branchOperationHoursPort.checkExist(COUNTRY,branchId)) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch does not exist in the system");
        }
    }
}