package capitec.branch.appointment.branch.domain;

import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.day.domain.DayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


public class Branch {

    @NotBlank
    private final String branchId;
    @NotNull
    private  LocalTime openTime;
    @NotNull
    private  LocalTime closingTime;
    private Map<DayType, BranchAppointmentInfo> branchAppointmentInfo;
    private Map<LocalDate,Set<Staff>> weeklyStaff;
    @NotNull
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

    public void setAddress(@NotNull Address address) {

        this.address = address;
        assert  address != null;
    }

    public void setBranchAppointmentInfo(@NotNull Map<DayType, BranchAppointmentInfo> branchAppointmentInfo) {
        this.branchAppointmentInfo = branchAppointmentInfo;
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
    public Map<DayType, BranchAppointmentInfo> getBranchAppointmentInfo() {
        return branchAppointmentInfo;
    }
    public Map<LocalDate,Set<Staff>> getWeeklyStaff() {
        return weeklyStaff;
    }
    public  void setWeeklyStaff( Map<LocalDate,Set<Staff>> weeklyStaff) {
        this.weeklyStaff = weeklyStaff;
    }

    public void addStaff(@NotNull LocalDate day,@NotNull Set<Staff> staff) {
        if (this.weeklyStaff == null) {
            this.weeklyStaff = new HashMap<>();

            this.weeklyStaff.put(day, staff);
            return;
        }

        Set<Staff> dailyStaff = this.weeklyStaff.get(day);
        dailyStaff.addAll(staff);

        this.weeklyStaff.put(day,dailyStaff);
    }
    public void addStaff(@NotNull LocalDate day,@NotNull Staff staff) {
        if (this.weeklyStaff == null) {
            this.weeklyStaff = new HashMap<>();
            Set<Staff> newStaff = new HashSet<>();
            newStaff.add(staff);
            this.weeklyStaff.put(day, newStaff);
            return;
        }

        Set<Staff> dailyStaff = this.weeklyStaff.get(day);

        if(dailyStaff==null){
            dailyStaff = new HashSet<>();
        }
        if(dailyStaff.contains(staff)){

            throw new DuplicateFormatFlagsException("Staff already exists on working staff at day: " + day.toString());
        }
        dailyStaff.add(staff);

        this.weeklyStaff.put(day,dailyStaff);
    }


    public void removeStaff(@NotNull LocalDate day,@NotNull Staff staff) {

        if (this.weeklyStaff == null) {
            throw  new IllegalArgumentException("Cannot remove a staff that has no weekly staff");
        }

        Set<Staff> dailyStaff = this.weeklyStaff.get(day);

        if(dailyStaff==null || dailyStaff.isEmpty()){

            throw  new IllegalArgumentException("Cannot remove a staff that has no daily staff");
        }
        if(!dailyStaff.contains(staff)){

            throw new NoSuchElementException("Cannot remove a staff that does not exist on working day: " + day.toString());
        }
        dailyStaff.remove(staff);

        this.weeklyStaff.put(day,dailyStaff);
    }



    public  void updateAppointmentInfo(@NotNull  DayType dayType , @NotNull BranchAppointmentInfo branchAppointmentInfo) {
        if(this.branchAppointmentInfo == null) {
            this.branchAppointmentInfo = new HashMap<>();
        }

        this.branchAppointmentInfo.put(dayType, branchAppointmentInfo);
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
                "branchId='" + branchId + '\'' +
                ", openTime=" + openTime +
                ", closingTime=" + closingTime +
                ", branchAppointmentInfo=" + branchAppointmentInfo +
                ", staff=" + weeklyStaff +
                ", address=" + address +
                '}';
    }
}
