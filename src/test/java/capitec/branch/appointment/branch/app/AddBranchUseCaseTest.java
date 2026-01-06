package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.address.Address;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class AddBranchUseCaseTest extends BranchTestBase {

    @Autowired
    private AddBranchUseCase addBranchUseCase; // The Use Case under test

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "BR001;09:00;17:00;123;Main Street;Rosebank;Johannesburg;Gauteng;2196;South Africa",
            "BR002;08:30;16:30;456;Church Street;Hatfield;Pretoria;Gauteng;2828;South Africa",
            "BR003;10:00;18:00;789;Long Street;City Centre;Cape Town;Western Cape;8001;South Africa"
    })
    public void testAddBranch_Success(String branchId, LocalTime openTime, LocalTime closingTime,
                                     String streetNumber, String streetName, String suburbs,
                                     String city, String province, Integer postalCode, String country) {

        // ARRANGE
        Address expectedAddress = new Address(streetNumber, streetName, suburbs, city, province, postalCode, country);
        BranchDTO branchDTO = createBranchDTO(branchId, openTime, closingTime, 
                                                streetNumber, streetName, suburbs, 
                                                city, province, postalCode, country);
        
        // ACT
        Branch branch = addBranchUseCase.execute(branchDTO);
        
        // ASSERT
        assertThat(branch).as("Branch should be created and not null").isNotNull();
        assertThat(branch)
            .as("Branch fields should match input DTO")
            .hasFieldOrPropertyWithValue("branchId", branchId)
            .hasFieldOrPropertyWithValue("openTime", openTime)
            .hasFieldOrPropertyWithValue("closingTime", closingTime)
            .hasFieldOrPropertyWithValue("address", expectedAddress)
            .hasFieldOrPropertyWithValue("branchAppointmentInfo", Collections.emptyList());
    }
    
    // TODO: Add tests for validation failure (e.g., null BranchDTO, invalid times)
}