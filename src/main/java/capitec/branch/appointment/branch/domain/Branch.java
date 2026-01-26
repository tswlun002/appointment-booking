package capitec.branch.appointment.branch.domain;

import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


public class Branch {

    @NotBlank(message = "Branch ID cannot be blank")
    private final String branchId;
    @NotBlank(message = "Branch name must have at least 2 letters")
    @Length(min = 2,message = "Branch name must have at least 2 letters ")
    private final String branchName;

    private List<BranchAppointmentInfo> branchAppointmentInfo;
    private List<OperationHoursOverride> operationHoursOverride;

    public Branch(String branchId, String branchName) {

        Assert.hasText(branchId,"Branch ID cannot be blank");
        Assert.hasText(branchName,"Branch Name cannot be blank");
        Assert.isTrue(branchName.length()>=2,"Branch name must have at least 2 letters");
        this.branchId = branchId;
        this.branchName = branchName;

    }

    private Branch(String branchId, String branchName, List<BranchAppointmentInfo> branchAppointmentInfo, List<OperationHoursOverride> operationHoursOverride) {
        this.branchId = branchId;
        this.branchName = branchName;
        this.branchAppointmentInfo = branchAppointmentInfo;
        this.operationHoursOverride = operationHoursOverride;
    }

    public static Branch restituteFromPersistence(String branchId, String branchName,List<BranchAppointmentInfo> branchAppointmentInfo, List<OperationHoursOverride> operationHoursOverride ) {
        Assert.hasText(branchId,"Branch ID cannot be blank");
        Assert.hasText(branchName,"Branch Name cannot be blank");
        return new Branch(branchId,branchName,branchAppointmentInfo,operationHoursOverride);
    }

    public void validateAppointmentInfoConsistency(@NotNull BranchAppointmentInfo info, LocalTime openTime, LocalTime closingTime) {

        Assert.notNull(info,"Branch Appointment Info cannot be null");
        Optional<OperationHoursOverride> optionalOperationHoursOverride = operationHoursOverride==null?Optional.empty(): operationHoursOverride
                .stream()
                .filter(h -> Objects.equals(h.effectiveDate(), info.day()))
                .findFirst();
        if(optionalOperationHoursOverride.isPresent()) {

            var h = optionalOperationHoursOverride.get();
            Assert.isTrue(!h.isExpired(), "Cannot update branch appointment info for for day already passed");
            Assert.isTrue(!h.closed(), "Cannot update branch appointment info for closed operation hours");
            boolean isLess = info.slotDuration().toMinutes() < Duration.between(h.openAt(), h.closeAt()).toMinutes();
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
        else {
            this.branchAppointmentInfo = new ArrayList<>(this.branchAppointmentInfo);
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

        validateDurationOverOpenOperationTime(operationHoursOverride);
        if(this.operationHoursOverride == null) {
            this.operationHoursOverride = new ArrayList<>();
        }
        else {
            this.operationHoursOverride = new ArrayList<>(this.operationHoursOverride);
        }

        this.operationHoursOverride.removeIf(h->Objects.equals(h.effectiveDate(), operationHoursOverride.effectiveDate()));
        this.operationHoursOverride.add(operationHoursOverride);
    }

    public void setOperationHoursOverride(@NotNull List<OperationHoursOverride> operationHoursOverride) {

        operationHoursOverride.forEach(this::validateDurationOverOpenOperationTime);
        this.operationHoursOverride = new ArrayList<>(operationHoursOverride);
    }


    private void  validateDurationOverOpenOperationTime(OperationHoursOverride operationHoursOverride) {
        if(this.branchAppointmentInfo != null && !this.branchAppointmentInfo.isEmpty()) {
            branchAppointmentInfo.stream()
                    .filter(op->
                            op.day().isEqual(operationHoursOverride.effectiveDate())
                    )
                    .findFirst()
                    .ifPresent(op->{

                        if(op.slotDuration().toMinutes()>=Duration.between(operationHoursOverride.openAt(), operationHoursOverride.closeAt()).toMinutes()) {

                            throw new IllegalArgumentException("Open operation time cannot be less than appointment slot duration time");
                        }
                    });


        }
    }

    public String getBranchId() {
        return branchId;
    }
    public String getBranchName() {return branchName;}

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
                ", branchName='" + branchName + '\'' +
                ", branchAppointmentInfo=" + branchAppointmentInfo +
                ", operationHoursOverride=" + operationHoursOverride +
                '}';
    }
}
