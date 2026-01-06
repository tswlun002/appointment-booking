package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static capitec.branch.appointment.staff.domain.StaffStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateStaffStatusUseCaseTest extends StaffUseCaseTestBase {

    @Autowired
    private AddStaffUseCase addStaffUseCase;
    @Autowired
    private UpdateStaffWorkStatusUseCase updateStaffWorkStatusUseCase;
    @Autowired
    private GetStaffInfoUseCase getStaffInfoUseCase;
    @Autowired private StaffService staffService;


    @BeforeEach
    void setupInitialStaff() {
        String branchId = "59f3c768-9712-423c-a940-9a873a4934fb";

        for (var username : staff) {
            StaffDTO staffDTO = new StaffDTO(username, branchId);
            addStaffUseCase.execute(staffDTO);
        }
    }



    @AfterEach
    public void tearDown() {
        for (String username : staff) {
            staffService.deleteStaff(username);
        }
    }


    @ParameterizedTest
    @ValueSource(strings = "59f3c768-9712-423c-a940-9a873a4934fb")
    void testUpdateStaffStatusExistingStaff(String branchId) {
        int halfStaffSize = staff.size() / 2;
        List<String> firstHalf = staff.subList(0, halfStaffSize);
        List<String> secondHalf = staff.subList(halfStaffSize, staff.size());

        // ACT 1: Update first half to WORKING
        for (var staffUser : firstHalf) {
            Staff updated = updateStaffWorkStatusUseCase.execute(staffUser, WORKING);
            assertThat(updated).isNotNull();
            assertThat(updated.status()).isEqualTo(WORKING);
        }
        
        // ACT 2: Update second half to LEAVE
        for (var staffUser : secondHalf) {
            Staff updated = updateStaffWorkStatusUseCase.execute(staffUser, LEAVE);
            assertThat(updated).isNotNull();
            assertThat(updated.status()).isEqualTo(LEAVE);
        }

        // VERIFY 1: WORKING staff count and content
        Set<String> workingStaff = getStaffInfoUseCase.getStaffUsernames(branchId, WORKING);
        assertThat(workingStaff).hasSize(firstHalf.size());
        assertThat(workingStaff).containsAll(firstHalf);

        // VERIFY 2: LEAVE staff count and content
        Set<String> leaveStaff = getStaffInfoUseCase.getStaffUsernames(branchId, LEAVE);
        assertThat(leaveStaff).hasSize(secondHalf.size());
       assertThat(leaveStaff).containsAll(secondHalf);
    }

    @ParameterizedTest
    @ValueSource(strings = "59f3c768-9712-423c-a940-9a873a4934fb")
    void testUpdateStaffStatusNonExistingStaffFails(String branchId) {
        String nonExistingUsername = "non_existent_user";
        
        // ASSERT: Attempting to update a user not in the staff table fails with 404
        assertThatThrownBy(() -> updateStaffWorkStatusUseCase.execute(nonExistingUsername, TRAINING))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND \"Staff not found.\"");

        // VERIFY: The staff list remains unchanged
        Set<String> allStaff = getStaffInfoUseCase.getStaffUsernames(branchId, TRAINING);
        assertThat(allStaff).containsAll(staff);
    }
}