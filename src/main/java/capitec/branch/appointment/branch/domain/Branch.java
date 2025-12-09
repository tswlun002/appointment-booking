package capitec.branch.appointment.branch.domain;

import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.exeption.InvalidAppointmentConfigurationException;
import capitec.branch.appointment.day.domain.DayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
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
    private List<BranchAppointmentInfo> branchAppointmentInfo;
    private Map<LocalDate,Set<StaffRef>> weeklyStaff;
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
    public void validateAppointmentInfoConsistency(@NotNull BranchAppointmentInfo info) {
        if (info.slotDuration().toMinutes()<= Duration.between(openTime, closingTime).toMinutes()) {
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
    public List<BranchAppointmentInfo> getBranchAppointmentInfo() {
        return branchAppointmentInfo == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(branchAppointmentInfo);
    }
    public Map<LocalDate,Set<StaffRef>> getWeeklyStaff() {
        if (weeklyStaff == null) {
            return Collections.emptyMap();
        }
        Map<LocalDate, Set<StaffRef>> copy = new HashMap<>();
        weeklyStaff.forEach((date, refs) ->
                copy.put(date, Set.copyOf(refs))
        );
        return Collections.unmodifiableMap(copy);
    }
    public  void setWeeklyStaff( Map<LocalDate,Set<StaffRef>> weeklyStaff) {
        this.weeklyStaff = weeklyStaff;
    }

    public void addStaff(@NotNull LocalDate day,@NotNull Set<StaffRef> staff) {
        if (this.weeklyStaff == null) {
            this.weeklyStaff = new HashMap<>();

            this.weeklyStaff.put(day, staff);
            return;
        }

        Set<StaffRef> dailyStaff = this.weeklyStaff.computeIfAbsent(day, k -> new HashSet<>());
        dailyStaff.addAll(staff);

        this.weeklyStaff.put(day,dailyStaff);
    }
    public void addStaff(@NotNull LocalDate day,@NotNull StaffRef staff) {
        if (this.weeklyStaff == null) {
            this.weeklyStaff = new HashMap<>();
            Set<StaffRef> newStaff = new HashSet<>();
            newStaff.add(staff);
            this.weeklyStaff.put(day, newStaff);
            return;
        }

        Set<StaffRef> dailyStaff = this.weeklyStaff.get(day);

        if(dailyStaff==null){
            dailyStaff = new HashSet<>();
        }
        if(dailyStaff.contains(staff)){

            throw new DuplicateFormatFlagsException("Staff already exists on working staff at day: " + day.toString());
        }
        dailyStaff.add(staff);

        this.weeklyStaff.put(day,dailyStaff);
    }


    public void removeStaff(@NotNull LocalDate day,@NotNull StaffRef staff) {

        if (this.weeklyStaff == null) {
            throw  new IllegalArgumentException("Cannot remove a staff that has no weekly staff");
        }

        Set<StaffRef> dailyStaff = this.weeklyStaff.get(day);

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
                "branchId='" + branchId + '\'' +
                ", openTime=" + openTime +
                ", closingTime=" + closingTime +
                ", branchAppointmentInfo=" + branchAppointmentInfo +
                ", staff=" + weeklyStaff +
                ", address=" + address +
                '}';
    }
}
