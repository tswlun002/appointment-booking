package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class AddBranchUseCaseTest extends BranchTestBase {

    @Autowired
    private AddBranchUseCase addBranchUseCase; // The Use Case under test

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "BR001",
            "BR002",
            "BR003"
    })
    public void testAddBranch_Success(String branchId) {

        // ARRANGE
        BranchDTO branchDTO = createBranchDTO(branchId);
        
        // ACT
        Branch branch = addBranchUseCase.execute(branchDTO);
        
        // ASSERT
        assertThat(branch).as("Branch should be created and not null").isNotNull();
        assertThat(branch)
            .as("Branch fields should match input DTO")
            .hasFieldOrPropertyWithValue("branchId", branchId)
            .hasFieldOrPropertyWithValue("branchAppointmentInfo", Collections.emptyList())
        .hasFieldOrPropertyWithValue("operationHoursOverride", Collections.emptyList());
    }
    
    // TODO: Add tests for validation failure (e.g., null BranchDTO, invalid times)
}