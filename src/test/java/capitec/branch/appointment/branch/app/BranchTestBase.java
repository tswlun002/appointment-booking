package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.address.Address;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for all Branch Use Case tests, handling application setup and global cleanup.
 *
 * NOTE: We autowire the specific Use Cases for cleanup since the original BranchUseCase
 * has been split. We need AddBranchUseCase, DeleteBranchUseCase, and GetAllBranchesQuery.
 */
abstract class BranchTestBase extends AppointmentBookingApplicationTests {

    // We must autowire the new Use Case implementations for setup/cleanup
    @Autowired
    protected DeleteBranchUseCase deleteBranchUseCase;
    @Autowired
    protected GetBranchQuery getAllBranchesQuery;

    // We keep the BranchService to perform the test logic for the specific test class.
    
    @AfterEach
    public void tearDown() {
        // Collect IDs before deletion to avoid modifying the collection while iterating.
        List<String> branchIds = getAllBranchesQuery.execute()
                .stream()
                .map(Branch::getBranchId)
                .collect(Collectors.toList());

        // Use the DeleteBranchUseCase to ensure cleanup is done via the Use Case boundary.
        branchIds.forEach(deleteBranchUseCase::execute);
    }

    /**
     * Helper method to create a BranchDTO from a CsvSource line.
     */
    protected BranchDTO createBranchDTO(String branchId, LocalTime openTime, LocalTime closingTime,
                                        String streetNumber, String streetName, String suburbs,
                                        String city, String province, Integer postalCode, String country) {
        
        Address address = new Address(streetNumber, streetName, suburbs, city, province, postalCode, country);
        return new BranchDTO(branchId, openTime, closingTime, address);
    }
}