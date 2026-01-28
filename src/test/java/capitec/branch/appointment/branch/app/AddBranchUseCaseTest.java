package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.infrastructure.dao.BranchDaoImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class AddBranchUseCaseTest extends BranchTestBase {

    @Autowired
    private AddBranchUseCase addBranchUseCase;

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "SAS293200",
            "SAS29300",
            "SASB9001"
    })
    public void testAddBranch_Success(String branchId) {

        stubCapitecApiSuccess(capitecApiWireMock,CAPITEC_BRANCH_API_RESPONSE);

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

        // confirm added on cache
        Cache cache = cacheManagerBranches.getCache(BranchDaoImpl.CACHE_NAME);
        Branch branch1 = cache.get(branchId,Branch.class);
        assertThat(branch1).isNotNull().isEqualTo(branch);
    }
    
    // TODO: Add tests for validation failure (e.g., null BranchDTO, invalid times)
}