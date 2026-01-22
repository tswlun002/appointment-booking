package capitec.branch.appointment.branch.domain;

import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


public class Branch {

    @NotBlank
    private final String branchId;
    private List<BranchAppointmentInfo> branchAppointmentInfo;
    private List<OperationHoursOverride> operationHoursOverride;

    public Branch(String branchId) {

        Assert.hasText(branchId,"Branch ID cannot be blank");
        this.branchId = branchId;

    }
    public void validateAppointmentInfoConsistency(@NotNull BranchAppointmentInfo info, LocalTime openTime, LocalTime closingTime) {

        Assert.notNull(info,"Branch Appointment Info cannot be null");
        Optional<OperationHoursOverride> optionalOperationHoursOverride = operationHoursOverride
                .stream()
                .filter(h -> Objects.equals(h.effectiveDate(), info.day()))
                .findFirst();
        if(optionalOperationHoursOverride.isPresent()) {

            var h = optionalOperationHoursOverride.get();
            Assert.isTrue(!h.isExpired(), "Cannot update branch appointment info for for day already passed");
            Assert.isTrue(!h.closed(), "Cannot update branch appointment info for closed operation hours");
            boolean isLess = info.slotDuration().toMinutes() < Duration.between(h.openTime(), h.closingTime()).toMinutes();
            Assert.isTrue(isLess, "Appointment slots must be within branch operating hours");

        }
        else{

            Assert.notNull(openTime,"Branch open time cannot be null");
            Assert.notNull(closingTime,"Branch closing time cannot be null");
            boolean isLess = info.slotDuration().toMinutes() < Duration.between(openTime, closingTime).toMinutes();
            Assert.isTrue(isLess, "Appointment slots must be within branch operating hours");
        }


    }

    public  void updateAppointmentInfo(@NotNull LocalDate day, @NotNull BranchAppointmentInfo branchAppointmentInfo, LocalTime openTime, LocalTime closingTime) {
        if(this.branchAppointmentInfo == null) {
            this.branchAppointmentInfo = new ArrayList<>();
        }

        validateAppointmentInfoConsistency(branchAppointmentInfo,openTime,closingTime);

        this.branchAppointmentInfo.removeIf(info -> info.day().equals(day));
        this.branchAppointmentInfo.add(branchAppointmentInfo);
    }
    public void setBranchAppointmentInfo(@NotNull List<BranchAppointmentInfo> branchAppointmentInfo, LocalTime openTime, LocalTime closingTime) {
        branchAppointmentInfo.forEach(b->this.validateAppointmentInfoConsistency(b,openTime,closingTime));
        this.branchAppointmentInfo = new ArrayList<>(branchAppointmentInfo);
    }
    public void updateOperationHoursOverride(@NotNull OperationHoursOverride operationHoursOverride) {

        if(this.operationHoursOverride == null) {
            this.operationHoursOverride = new ArrayList<>();
        }

        this.operationHoursOverride.removeIf(h->Objects.equals(h.effectiveDate(), operationHoursOverride.effectiveDate()));
        this.operationHoursOverride.add(operationHoursOverride);
    }
    public void setOperationHoursOverride(@NotNull List<OperationHoursOverride> operationHoursOverride) {
        this.operationHoursOverride = new ArrayList<>(operationHoursOverride);
    }

    public String getBranchId() {
        return branchId;
    }

    public List<BranchAppointmentInfo> getBranchAppointmentInfo() {
        return branchAppointmentInfo == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(branchAppointmentInfo);
    }
    public List<OperationHoursOverride> getOperationHoursOverride() {
        return operationHoursOverride == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(operationHoursOverride);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Branch branch)) return false;
        return Objects.equals(branchId, branch.branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchId);
    }

    @Override
    public String toString() {
        return "Branch{" +
                "username='" + branchId + '\'' +
                ", branchAppointmentInfo=" + branchAppointmentInfo +
                ", operationHoursOverride=" + operationHoursOverride +
                '}';
    }
}
