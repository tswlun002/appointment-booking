package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import org.junit.jupiter.api.Test;
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

    @Test
    void testAddStaff_Successfully() {
        // ACT & ASSERT: Add all pre-created users as staff
        for (var username : staff) {
            StaffDTO staffDTO = new StaffDTO(username, branch.getBranchId());
            // We call the single execute method now
            boolean isAdded = addStaffUseCase.execute(staffDTO); 
            assertThat(isAdded).isTrue();
        }

        // VERIFY: Check the count using the query use case
        int staffCount = getStaffInfoUseCase.getStaffUsernames(branch.getBranchId(), TRAINING).size();
        assertThat(staffCount).isEqualTo(staff.size());
    }
    @Test
    void throwException_AddStaff_DuplicateStaff() {
        // ACT & ASSERT: Add all pre-created users as staff
        for (var username : staff) {
            StaffDTO staffDTO = new StaffDTO(username, branch.getBranchId());
            // We call the single execute method now
            boolean isAdded = addStaffUseCase.execute(staffDTO);
             assertThatThrownBy(()->addStaffUseCase.execute(staffDTO))
                     .hasCauseInstanceOf(EntityAlreadyExistException.class)
                     .isInstanceOf(ResponseStatusException.class)
                     .hasMessageContaining("Staff already exists in this branch.");

            assertThat(isAdded).isTrue();
        }

        // VERIFY: Check the count using the query use case
        int staffCount = getStaffInfoUseCase.getStaffUsernames(branch.getBranchId(), TRAINING).size();
        assertThat(staffCount).isEqualTo(staff.size());
    }
}