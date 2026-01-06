package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.address.Address;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
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
            "BR001;09:00;17:00;123;Main Street;Rosebank;Johannesburg;Gauteng;2196;South Africa",
            "BR002;08:30;16:30;456;Church Street;Hatfield;Pretoria;Gauteng;2828;South Africa"
    })
    public void testGetBranchByIdQuery_ExistingBranch(String branchId, LocalTime openTime, LocalTime closingTime,
                                                      String streetNumber, String streetName, String suburbs,
                                                      String city, String province, Integer postalCode, String country) {

        // ARRANGE: Add the branch first
        Address expectedAddress = new Address(streetNumber, streetName, suburbs, city, province, postalCode, country);
        BranchDTO branchDTO = createBranchDTO(branchId, openTime, closingTime, 
                                                streetNumber, streetName, suburbs, 
                                                city, province, postalCode, country);
        addBranchUseCase.execute(branchDTO);
        
        // ACT
        Branch branch = getBranchByIdQuery.execute(branchId);
        
        // ASSERT (Same assertions as original getExistingBranch test)
        assertThat(branch).as("Retrieved branch should not be null").isNotNull();
        assertThat(branch)
                .as("Retrieved branch fields should match created fields")
                .hasFieldOrPropertyWithValue("branchId", branchId)
                .hasFieldOrPropertyWithValue("openTime", openTime)
                .hasFieldOrPropertyWithValue("closingTime", closingTime)
                .hasFieldOrPropertyWithValue("address", expectedAddress)
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
        BranchDTO dto1 = createBranchDTO("B1", LocalTime.NOON, LocalTime.MIDNIGHT.minusMinutes(1), "1", "A", "S", "C", "P", 1, "SA");
        BranchDTO dto2 = createBranchDTO("B2", LocalTime.NOON, LocalTime.MIDNIGHT.minusMinutes(1), "2", "B", "S", "C", "P", 2, "SA");
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