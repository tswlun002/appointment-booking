package capitec.branch.appointment.branch.domain;

import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.exeption.InvalidAppointmentConfigurationException;
import capitec.branch.appointment.day.domain.DayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;


public class Branch {

    @NotBlank
    private final String branchId;
    @NotNull
    private  LocalTime openTime;
    @NotNull
    private  LocalTime closingTime;
    private List<BranchAppointmentInfo> branchAppointmentInfo;
    private Address address;

    public Branch(String branchId, LocalTime openTime, LocalTime closingTime,Address address) {

        this.branchId = branchId;
        this.openTime = openTime;
        this.closingTime = closingTime;
        this.address = address;
        assert  branchId != null;
        assert openTime != null;
        assert closingTime != null;
        assert openTime.isBefore(closingTime);
        assert  address != null;

    }
    public void validateAppointmentInfoConsistency(@NotNull BranchAppointmentInfo info) {
        if (info.slotDuration().toMinutes()>Duration.between(openTime, closingTime).toMinutes()) {
            throw new InvalidAppointmentConfigurationException(
                    "Appointment slots must be within branch operating hours"
            );
        }
    }

    public void setAddress(@NotNull Address address) {

        this.address = address;
        assert  address != null;
    }

    public void setBranchAppointmentInfo(@NotNull List<BranchAppointmentInfo> branchAppointmentInfo) {
        branchAppointmentInfo.forEach(this::validateAppointmentInfoConsistency);
        this.branchAppointmentInfo = new ArrayList<>(branchAppointmentInfo);
    }
    public  Address getAddress() {
        return address;
    }
    public String getBranchId() {
        return branchId;
    }
    public LocalTime getOpenTime() {
        return openTime;
    }
    public LocalTime getClosingTime() {
        return closingTime;
    }
    public List<BranchAppointmentInfo> getBranchAppointmentInfo() {
        return branchAppointmentInfo == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(branchAppointmentInfo);
    }




    public  void updateAppointmentInfo(@NotNull  DayType dayType , @NotNull BranchAppointmentInfo branchAppointmentInfo) {
        if(this.branchAppointmentInfo == null) {
            this.branchAppointmentInfo = new ArrayList<>();
        }

        validateAppointmentInfoConsistency(branchAppointmentInfo);

        this.branchAppointmentInfo.removeIf(info -> info.dayType().equals(dayType));
        this.branchAppointmentInfo.add(branchAppointmentInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Branch branch)) return false;
        return Objects.equals(branchId, branch.branchId) && Objects.equals(openTime, branch.openTime) && Objects.equals(closingTime, branch.closingTime) && Objects.equals(address, branch.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchId, openTime, closingTime, address);
    }

    @Override
    public String toString() {
        return "Branch{" +
                "username='" + branchId + '\'' +
                ", openTime=" + openTime +
                ", closingTime=" + closingTime +
                ", branchAppointmentInfo=" + branchAppointmentInfo +
                ", address=" + address +
                '}';
    }
}
