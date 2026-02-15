package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.app.port.BranchDetails;
import capitec.branch.appointment.branch.app.port.BranchOperationHoursPort;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

/**
 * Use case for registering a new branch in the appointment booking system.
 *
 * <p>This use case validates that the branch exists in the Capitec Branch Locator system
 * before adding it to the local database. This ensures only valid Capitec branches
 * can be configured for appointment booking.</p>
 *
 * <h2>Input ({@link BranchDTO}):</h2>
 * <ul>
 *   <li><b>branchId</b> - The unique branch code from Capitec Branch Locator API (e.g., "470010")</li>
 * </ul>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Validates the branch exists in Capitec Branch Locator API for South Africa</li>
 *   <li>Retrieves branch details (name) from the API</li>
 *   <li>Creates a {@link Branch} domain object with the branchId and name</li>
 *   <li>Persists the branch to the database</li>
 * </ol>
 *
 * <h2>Business Rules:</h2>
 * <ul>
 *   <li>Branch must exist in Capitec Branch Locator system</li>
 *   <li>Branch ID must be valid and not blank</li>
 *   <li>Duplicate branches cannot be added (enforced by database)</li>
 * </ul>
 *
 * <h2>Example Use Case:</h2>
 * <p>Admin registers a new branch for appointment booking:</p>
 * <pre>
 * {
 *   "branchId": "470010"
 * }
 * </pre>
 * <p>The system will:</p>
 * <ol>
 *   <li>Call Branch Locator API to verify branch 470010 exists</li>
 *   <li>Retrieve branch name: "Rondebosch"</li>
 *   <li>Save branch with ID "470010" and name "Rondebosch" to database</li>
 * </ol>
 *
 * @see Branch
 * @see BranchDTO
 * @see BranchService
 * @see BranchOperationHoursPort
 */
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

           BranchDetails branchDetails = getBranchDetails(branchInput.branchId());

           Branch branch = new Branch(branchInput.branchId(),branchDetails.branchName());
           return branchService.add(branch);


       } catch (ResponseStatusException e) {
           throw e ;
       }
       catch (IllegalArgumentException | IllegalStateException | ConstraintViolationException e) {
           log.warn("Invalid branch input: {}", branchInput, e);
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(),e);
       }
       catch (Exception e) {
           log.warn("Unable to add branch with ID: {}", branchInput.branchId(), e);
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error while adding branch.");
       }

    }

    private BranchDetails getBranchDetails(String branchId){
        return branchOperationHoursPort.getBranchNames(COUNTRY,branchId)
                .orElseThrow(()->{
            log.error("Branch with ID {} does not exist", branchId);
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch does not exist in the system");
        });
    }
}