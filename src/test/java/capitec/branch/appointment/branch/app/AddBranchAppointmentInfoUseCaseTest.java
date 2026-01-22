package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddBranchAppointmentInfoUseCaseTest extends BranchTestBase {

    @Autowired
    private AddBranchUseCase addBranchUseCase; // For setup
    @Autowired
    private GetBranchQuery getBranchByIdQuery; // For verification
    @Autowired
    private AddBranchAppointmentInfoUseCase addBranchAppointmentInfoUseCase; // Use Case under test


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "BR001;30;0.5;5",
            "BR002;20;0.6;5"
    })
    public void testUpdateBranchAppointmentConfigUseCase_AddsNewConfig(String branchId, Integer duration, Double utilizationFactor, int staffCount) {
        LocalDate day = LocalDate.now();

        // ARRANGE: Create the branch first
        BranchDTO branchDTO = createBranchDTO(branchId);
        addBranchUseCase.execute(branchDTO);

        BranchAppointmentInfoDTO configDTO = new BranchAppointmentInfoDTO(staffCount, 
                                                                           Duration.ofMinutes(duration), 
                                                                           utilizationFactor,
                day);
        
        // ACT
        boolean isAdded = addBranchAppointmentInfoUseCase.execute(branchId, configDTO);
        
        // ASSERT 1: The use case reported successful addition
        assertThat(isAdded).isTrue();
        
        // ASSERT 2: Verify the data was saved and updated on the Branch object
        Branch branch = getBranchByIdQuery.execute(branchId);
        assertThat(branch).as("Branch should exist after update").isNotNull();
        
        List<BranchAppointmentInfo> appointmentInfo = branch.getBranchAppointmentInfo();
        assertThat(appointmentInfo).as("Appointment info list should not be null").isNotNull();
        
        boolean dayTypeConfigAdded = appointmentInfo.stream().anyMatch(a -> a.day().equals(day) &&
                                                                           a.staffCount() == staffCount &&
                                                                           a.slotDuration().toMinutes() == duration);
                                                                           
        assertThat(dayTypeConfigAdded).as("The specific DayType config should have been added").isTrue();
    }
    
    @Test
    void testUpdateBranchAppointmentConfigUseCase_NonExistingBranch_ThrowsNotFound() {
        // ARRANGE
        BranchAppointmentInfoDTO configDTO = new BranchAppointmentInfoDTO(5, Duration.ofMinutes(30), 0.5, LocalDate.now());

        // ACT & ASSERT
        assertThatThrownBy(() -> addBranchAppointmentInfoUseCase.execute("NON_EXISTENT_ID", configDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Branch not found");
    }
}