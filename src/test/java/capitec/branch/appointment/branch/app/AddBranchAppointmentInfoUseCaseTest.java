package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
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
    private AddBranchUseCase addBranchUseCase;
    @Autowired
    private GetBranchQuery getBranchByIdQuery;
    @Autowired
    private AddBranchAppointmentInfoUseCase addBranchAppointmentInfoUseCase; // Use Case under test


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "SAS29300;30;0.5;5",
            "SASB9001;20;0.6;5"
    })
    public void testUpdateBranchAppointmentConfigUseCase_AddsNewConfig(String branchId, Integer duration, Double utilizationFactor, int staffCount) {
        LocalDate day = LocalDate.now();

        // ARRANGE: Create the branch first
        stubCapitecApiSuccess(capitecApiWireMock,CAPITEC_BRANCH_API_RESPONSE);
        BranchDTO branchDTO = createBranchDTO(branchId);
        addBranchUseCase.execute(branchDTO);

        BranchAppointmentInfoDTO configDTO = new BranchAppointmentInfoDTO(staffCount, 
                                                                           Duration.ofMinutes(duration), 
                                                                           utilizationFactor,
                DayType.valueOf(day.getDayOfWeek().name()),2);
        
        // ACT
        boolean isAdded = addBranchAppointmentInfoUseCase.execute(branchId, configDTO);
        
        // ASSERT 1: The use case reported successful addition
        assertThat(isAdded).isTrue();
        
        // ASSERT 2: Verify the data was saved and updated on the Branch object
        Branch branch = getBranchByIdQuery.execute(branchId);
        assertThat(branch).as("Branch should exist after update").isNotNull();
        
        List<BranchAppointmentInfo> appointmentInfo = branch.getBranchAppointmentInfo();
        assertThat(appointmentInfo).as("Appointment info list should not be null").isNotNull();
        
        boolean dayTypeConfigAdded = appointmentInfo.stream().anyMatch(a -> a.day().name().equals(day.getDayOfWeek().name()) &&
                                                                           a.staffCount() == staffCount &&
                                                                           a.slotDuration().toMinutes() == duration);
                                                                           
        assertThat(dayTypeConfigAdded).as("The specific DayType config should have been added").isTrue();
    }
    
    @Test
    void testUpdateBranchAppointmentConfigUseCase_NonExistingBranch_ThrowsNotFound() {
        // ARRANGE
        LocalDate now = LocalDate.now();
        BranchAppointmentInfoDTO configDTO = new BranchAppointmentInfoDTO(5, Duration.ofMinutes(30), 0.5, DayType.valueOf(now.getDayOfWeek().name()),2);

        // ACT & ASSERT
        assertThatThrownBy(() -> addBranchAppointmentInfoUseCase.execute("SAS29300", configDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Branch not found");
    }
}