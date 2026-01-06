package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.day.domain.DayType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateBranchAppointmentConfigUseCaseTest extends BranchTestBase {

    @Autowired
    private AddBranchUseCase addBranchUseCase; // For setup
    @Autowired
    private GetBranchQuery getBranchByIdQuery; // For verification
    @Autowired
    private UpdateBranchAppointmentConfigUseCase updateBranchAppointmentConfigUseCase; // Use Case under test


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "BR001;09:00;17:00;123;Main Street;Rosebank;Johannesburg;Gauteng;2196;South Africa;30;0.5;5;WEEK_DAYS",
            "BR002;08:30;16:30;456;Church Street;Hatfield;Pretoria;Gauteng;2828;South Africa;20;0.6;5;WEEKEND"
    })
    public void testUpdateBranchAppointmentConfigUseCase_AddsNewConfig(String branchId, LocalTime openTime, LocalTime closingTime,
                                                                       String streetNumber, String streetName, String suburbs,
                                                                       String city, String province, Integer postalCode, String country,
                                                                       Integer duration, Double utilizationFactor, int staffCount, DayType dayType) {

        // ARRANGE: Create the branch first
        BranchDTO branchDTO = createBranchDTO(branchId, openTime, closingTime, 
                                                streetNumber, streetName, suburbs, 
                                                city, province, postalCode, country);
        addBranchUseCase.execute(branchDTO);

        BranchAppointmentInfoDTO configDTO = new BranchAppointmentInfoDTO(staffCount, 
                                                                           Duration.ofMinutes(duration), 
                                                                           utilizationFactor, 
                                                                           dayType);
        
        // ACT
        boolean isAdded = updateBranchAppointmentConfigUseCase.execute(branchId, configDTO);
        
        // ASSERT 1: The use case reported successful addition
        assertThat(isAdded).isTrue();
        
        // ASSERT 2: Verify the data was saved and updated on the Branch object
        Branch branch = getBranchByIdQuery.execute(branchId);
        assertThat(branch).as("Branch should exist after update").isNotNull();
        
        List<BranchAppointmentInfo> appointmentInfo = branch.getBranchAppointmentInfo();
        assertThat(appointmentInfo).as("Appointment info list should not be null").isNotNull();
        
        boolean dayTypeConfigAdded = appointmentInfo.stream().anyMatch(a -> a.dayType().equals(dayType) &&
                                                                           a.staffCount() == staffCount &&
                                                                           a.slotDuration().toMinutes() == duration);
                                                                           
        assertThat(dayTypeConfigAdded).as("The specific DayType config should have been added").isTrue();
    }
    
    @Test
    void testUpdateBranchAppointmentConfigUseCase_NonExistingBranch_ThrowsNotFound() {
        // ARRANGE
        BranchAppointmentInfoDTO configDTO = new BranchAppointmentInfoDTO(5, Duration.ofMinutes(30), 0.5, DayType.WEEK_DAYS);

        // ACT & ASSERT
        assertThatThrownBy(() -> updateBranchAppointmentConfigUseCase.execute("NON_EXISTENT_ID", configDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Branch not found");
    }
}