package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.staff.domain.StaffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import static capitec.branch.appointment.staff.domain.StaffStatus.TRAINING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddStaffUseCaseTest extends StaffUseCaseTestBase {

    @Autowired
    private AddStaffUseCase addStaffUseCase;

    @Autowired
    private GetStaffInfoUseCase getStaffInfoUseCase;
    @Autowired private StaffService staffService;


    @AfterEach
    public void tearDown() {
        for (String username : staff) {
            staffService.deleteStaff(username);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = "59f3c768-9712-423c-a940-9a873a4934fb")
    void testAddStaffSuccessfully(String branchId) {
        // ACT & ASSERT: Add all pre-created users as staff
        for (var username : staff) {
            StaffDTO staffDTO = new StaffDTO(username, branchId);
            // We call the single execute method now
            boolean isAdded = addStaffUseCase.execute(staffDTO); 
            assertThat(isAdded).isTrue();
        }

        // VERIFY: Check the count using the query use case
        int staffCount = getStaffInfoUseCase.getStaffUsernames(branchId, TRAINING).size();
        assertThat(staffCount).isEqualTo(staff.size());
    }
    @ParameterizedTest
    @ValueSource(strings = "59f3c768-9712-423c-a940-9a873a4934fb")
    void testAddStaffDuplicateStaff(String branchId) {
        // ACT & ASSERT: Add all pre-created users as staff
        for (var username : staff) {
            StaffDTO staffDTO = new StaffDTO(username, branchId);
            // We call the single execute method now
            boolean isAdded = addStaffUseCase.execute(staffDTO);
             assertThatThrownBy(()->addStaffUseCase.execute(staffDTO))
                     .hasCauseInstanceOf(EntityAlreadyExistException.class)
                     .isInstanceOf(ResponseStatusException.class)
                     .hasMessageContaining("Staff already exists in this branch.");

            assertThat(isAdded).isTrue();
        }

        // VERIFY: Check the count using the query use case
        int staffCount = getStaffInfoUseCase.getStaffUsernames(branchId, TRAINING).size();
        assertThat(staffCount).isEqualTo(staff.size());
    }
}