package capitec.branch.appointment.branch.app;


import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfoService;
import capitec.branch.appointment.staff.app.AvailableStaff;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@UseCase
@Slf4j
@Validated
@RequiredArgsConstructor
public class BranchUseCase {

    private final BranchService addBranch;
    private final BranchAppointmentInfoService branchAppointmentInfoService;
    private final AvailableStaff AvailableStaff;

    public boolean addBranch(@Valid BranchDTO branchInput) {

        if (branchInput == null) {
            log.error("branch is null");
            throw new IllegalArgumentException("branch is null, it can not added");
        }

        Branch branch = new Branch(branchInput.branchId(), branchInput.openTime(), branchInput.closingTime(), branchInput.address());

        var isAdded = false;
       try {

           addBranch.add(branch);
           isAdded = true;

       }catch (Exception e) {

           log.warn("Unable to add branch.", e);
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
       }
       return isAdded;
    }


    public boolean AddWorkingStaff(String branchId,@Valid BranchAppointmentInfoDTO branchAppointmentInfo) {

        var  branch = addBranch.getByBranchId(branchId).orElseThrow(() ->{
            log.error("Unable to find branch with id {}", branchId);
           return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
        });

        int staffCount = AvailableStaff.staffCount(branchId);

        //WE CAN CREATE EVENT TO ADMIN ABOUT SHORTAGE OF STAFF IF THE COUNT IS ZERO/(DOES NOT CERTAIN MINIMUM)

        BranchAppointmentInfo info = new BranchAppointmentInfo(branchAppointmentInfo.slotDuration(),
                branchAppointmentInfo.utilizationFactor(), staffCount,branchAppointmentInfo.dayType());

        branch.updateAppointmentInfo(branchAppointmentInfo.dayType(),info);


        try {

           return branchAppointmentInfoService.addBranchAppointmentConfigInfo(branchAppointmentInfo.dayType(),branch);

        } catch (Exception e) {

            log.warn("Unable to add branch.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

}
