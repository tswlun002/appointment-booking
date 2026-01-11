package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.staff.domain.StaffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static capitec.branch.appointment.staff.domain.StaffStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class GetStaffInfoUseCaseTest extends StaffUseCaseTestBase {

    @Autowired
    private AddStaffUseCase addStaffUseCase;
    @Autowired
    private UpdateStaffWorkStatusUseCase updateStaffWorkStatusUseCase;
    @Autowired
    private  GetStaffInfoUseCase getStaffInfoUseCase;
    @Autowired
    private StaffService staffService;
    private static List<String> staffFirstHalf;
    private static List<String> staffSecondHalf;



    /**
     * Set up staff data: Add all staff, then split and set half to WORKING and half to LEAVE.
     */
    @BeforeEach
    void setupStaffStatusesForQueryTests() {
        // 1. Add all staff (default status: TRAINING)


            for (var username : staff) {
                StaffDTO staffDTO = new StaffDTO(username, branch.getBranchId());
                addStaffUseCase.execute(staffDTO);
            }

            int halfSize = staff.size() / 2;
            staffFirstHalf = staff.subList(0, halfSize);
            staffSecondHalf = staff.subList(halfSize, staff.size());

            // 2. Set statuses
            for (var username : staffFirstHalf) {
                updateStaffWorkStatusUseCase.execute(username, WORKING);
            }
            for (var username : staffSecondHalf) {
                updateStaffWorkStatusUseCase.execute(username, LEAVE);
            }
      ;
    }

    @AfterEach
    public void tearDown() {
        for (String username : staff) {
            staffService.deleteStaff(username);
        }
    }


    @Test
    void testGetStaffCountReturnsCorrectWorkingStaffCount() {
        // ACT: Call the method that counts staff with WORKING status
        int workingStaffCount = getStaffInfoUseCase.getStaffCount(branch.getBranchId());

        // ASSERT: Should match the size of the first half
        assertThat(workingStaffCount).isEqualTo(staffFirstHalf.size());
    }

    @Test
    void testGetStaffUsernamesReturnsCorrectSetForWorkingStatus() {
        // ACT: Get the set of usernames for WORKING staff
        Set<String> resultUsernames = getStaffInfoUseCase.getStaffUsernames(branch.getBranchId(), WORKING);

        // ASSERT: Result set should be exactly the first half of staff
        assertThat(resultUsernames)
            .hasSize(staffFirstHalf.size())
            .containsExactlyInAnyOrderElementsOf(staffFirstHalf);
    }
    
    @Test
    void testGetStaffUsernamesReturnsCorrectSetForLeaveStatus() {
        // ACT: Get the set of usernames for LEAVE staff
        Set<String> resultUsernames = getStaffInfoUseCase.getStaffUsernames(branch.getBranchId(), LEAVE);

        // ASSERT: Result set should be exactly the second half of staff
        assertThat(resultUsernames)
            .hasSize(staffSecondHalf.size())
            .containsExactlyInAnyOrderElementsOf(staffSecondHalf);
    }
    
    @Test

    void testGetStaffUsernamesReturnsEmptySetForUnusedStatus() {
        // ACT: Get the set of usernames for a status we didn't set (e.g., TRAINING)
        Set<String> resultUsernames = getStaffInfoUseCase.getStaffUsernames(branch.getBranchId(), TRAINING);

        // ASSERT: Result set should be empty
        assertThat(resultUsernames).isEmpty();
    }
}