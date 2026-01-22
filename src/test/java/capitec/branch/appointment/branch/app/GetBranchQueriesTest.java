package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetBranchQueriesTest extends BranchTestBase {

    @Autowired
    private AddBranchUseCase addBranchUseCase; // For setup
    @Autowired
    private GetBranchQuery getBranchByIdQuery; // Query under test 1

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "BR001",
            "BR002"
    })
    public void testGetBranchByIdQuery_ExistingBranch(String branchId) {

        // ARRANGE: Add the branch first
        BranchDTO branchDTO = createBranchDTO(branchId);
        addBranchUseCase.execute(branchDTO);
        
        // ACT
        Branch branch = getBranchByIdQuery.execute(branchId);
        
        // ASSERT (Same assertions as original getExistingBranch test)
        assertThat(branch).as("Retrieved branch should not be null").isNotNull();
        assertThat(branch)
                .as("Retrieved branch fields should match created fields")
                .hasFieldOrPropertyWithValue("branchId", branchId)
                .hasFieldOrPropertyWithValue("operationHoursOverride", Collections.emptyList())
                .hasFieldOrPropertyWithValue("branchAppointmentInfo", Collections.emptyList());
    }

    @Test
    void testGetBranchByIdQuery_NonExistingBranch_ThrowsNotFound() {
        // ACT & ASSERT
        assertThatThrownBy(() -> getBranchByIdQuery.execute("NON_EXISTENT_ID"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Branch not found");
    }

    @Test
    void testGetAllBranchesQuery_ReturnsAllCreatedBranches() {
        // ARRANGE: Add two known branches
        BranchDTO dto1 = createBranchDTO("B1");
        BranchDTO dto2 = createBranchDTO("B2");
        addBranchUseCase.execute(dto1);
        addBranchUseCase.execute(dto2);
        
        // ACT
        var allBranches = getAllBranchesQuery.execute();
        
        // ASSERT
        assertThat(allBranches)
            .as("Should retrieve exactly two branches")
            .hasSize(2);
        
        assertThat(allBranches.stream().map(Branch::getBranchId))
            .as("Retrieved IDs must match the created IDs")
            .containsExactlyInAnyOrder("B1", "B2");
    }
}