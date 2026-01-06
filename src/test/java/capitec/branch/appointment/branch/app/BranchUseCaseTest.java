package capitec.branch.appointment.branch.app;


import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.day.domain.DayType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BranchUseCaseTest  extends AppointmentBookingApplicationTests {

    @Autowired
    private BranchUseCase branchUseCase;
    @AfterEach
    public void tearDown() {

        branchUseCase.getAllBranch()
                .stream()
                .map(Branch::getBranchId)
                .forEach(branchUseCase::deleteBranch);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "BR001;09:00;17:00;123;Main Street;Rosebank;Johannesburg;Gauteng;2196;South Africa",
            "BR002;08:30;16:30;456;Church Street;Hatfield;Pretoria;Gauteng;2828;South Africa",
            "BR003;10:00;18:00;789;Long Street;City Centre;Cape Town;Western Cape;8001;South Africa"
    })
    public void addBranch(String branchId, LocalTime openTime, LocalTime closingTime,
                          String streetNumber, String streetName, String suburbs,
                          String city, String province, Integer postalCode, String country) {



        Address address = new Address(streetNumber, streetName, suburbs, city, province, postalCode, country);
        BranchDTO branchDTO = new BranchDTO(branchId, openTime, closingTime, address);
        Branch branch = branchUseCase.addBranch(branchDTO);
        // Assert
        assertThat(branch).isNotNull();
        assertThat(branch).
                hasFieldOrPropertyWithValue("branchId", branchId)
                .hasFieldOrPropertyWithValue("openTime", openTime)
                .hasFieldOrPropertyWithValue("closingTime", closingTime)
                .hasFieldOrPropertyWithValue("address", address)
                .hasFieldOrPropertyWithValue("branchAppointmentInfo", Collections.emptyList());

    }
    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "BR001;09:00;17:00;123;Main Street;Rosebank;Johannesburg;Gauteng;2196;South Africa",
            "BR002;08:30;16:30;456;Church Street;Hatfield;Pretoria;Gauteng;2828;South Africa",
            "BR003;10:00;18:00;789;Long Street;City Centre;Cape Town;Western Cape;8001;South Africa"
    })
    public void getExistingBranch(String branchId, LocalTime openTime, LocalTime closingTime,
                          String streetNumber, String streetName, String suburbs,
                          String city, String province, Integer postalCode, String country) {



        Address address = new Address(streetNumber, streetName, suburbs, city, province, postalCode, country);
        BranchDTO branchDTO = new BranchDTO(branchId, openTime, closingTime, address);
        branchUseCase.addBranch(branchDTO);
        Branch branch  = branchUseCase.getBranch(branchId);
        // Assert
        assertThat(branch).isNotNull();
        assertThat(branch).
                hasFieldOrPropertyWithValue("branchId", branchId)
                .hasFieldOrPropertyWithValue("openTime", openTime)
                .hasFieldOrPropertyWithValue("closingTime", closingTime)
                .hasFieldOrPropertyWithValue("address", address)
                .hasFieldOrPropertyWithValue("branchAppointmentInfo", Collections.emptyList());

    }



    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "BR001;09:00;17:00;123;Main Street;Rosebank;Johannesburg;Gauteng;2196;South Africa;30;0.5;5;WEEK_DAYS",
            "BR002;08:30;16:30;456;Church Street;Hatfield;Pretoria;Gauteng;2828;South Africa;30;0.6;5;WEEK_DAYS",
            "BR003;10:00;18:00;789;Long Street;City Centre;Cape Town;Western Cape;8001;South Africa;30;0.7;4;WEEK_DAYS"
    })
    public void AddBranchAppointmentInfo(String branchId, LocalTime openTime, LocalTime closingTime,
                                String streetNumber, String streetName, String suburbs,
                                String city, String province, Integer postalCode, String country,
                                Integer duration, Double utilizationFactor,int staffCount, DayType dayType) {

//        for (var staff : staff) {
//            StaffDTO staffDTO = new StaffDTO(staff, branchId);
//            boolean added = staffUseCase.addStaff(staffDTO);
//           added = added && staffUseCase.updateStaff(staff, StaffStatus.WORKING)!=null;
//
//            if(!added){
//               throw new RuntimeException("Failed to add staff for testing to add working staff");
//            }
//        }

        Address address = new Address(streetNumber, streetName, suburbs, city, province, postalCode, country);
        BranchDTO branchDTO = new BranchDTO(branchId, openTime, closingTime, address);
        branchUseCase.addBranch(branchDTO);

        BranchAppointmentInfoDTO branchAppointmentInfo = new BranchAppointmentInfoDTO(staffCount,Duration.ofMinutes(duration), utilizationFactor, dayType);
        boolean isAdded = branchUseCase.addBranchAppointmentConfigInfo(branchId, branchAppointmentInfo);
        assertThat(isAdded).isTrue();
        Branch branch = branchUseCase.getBranch(branchId);
        assertThat(branch).isNotNull();
        List<BranchAppointmentInfo> appointmentInfo = branch.getBranchAppointmentInfo();
        assertThat(appointmentInfo).isNotNull();
        boolean dayTypeConfigAdded = appointmentInfo.stream().anyMatch(a -> a.dayType().equals(dayType));
        assertThat(dayTypeConfigAdded).isTrue();
    }



}