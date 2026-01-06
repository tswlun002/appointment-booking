package capitec.branch.appointment.branch.app;


import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfoService;

import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;

@UseCase
@Slf4j
@Validated
@RequiredArgsConstructor
public class BranchUseCase {

    private final BranchService branchService;
    private final BranchAppointmentInfoService branchAppointmentInfoService;
    //private final AvailableStaff availableStaff;
   // private final StaffSchedule staffSchedule;


    public Branch addBranch(@Valid BranchDTO branchInput) {

        if (branchInput == null) {
            log.error("branch is null");
            throw new IllegalArgumentException("branch is null, it can not added");
        }

        Branch branch = new Branch(branchInput.branchId(), branchInput.openTime(), branchInput.closingTime(), branchInput.address());

       try {

           branch = branchService.add(branch);


       }catch (Exception e) {

           log.warn("Unable to add branch.", e);
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
       }
        return branch;
    }

    public Branch getBranch(String branchId) {

        return branchService.getByBranchId(branchId).orElseThrow(() -> {

            log.warn("Unable to find branch with id {}", branchId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");

        });
    }


    public boolean addBranchAppointmentConfigInfo(String branchId, @Valid BranchAppointmentInfoDTO branchAppointmentInfo) {

        var  branch = branchService.getByBranchId(branchId).orElseThrow(() ->{
            log.error("Unable to find branch with id {}", branchId);
           return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
        });



        //WE CAN CREATE EVENT TO ADMIN ABOUT SHORTAGE OF STAFF IF THE COUNT IS ZERO/(DOES NOT CERTAIN MINIMUM)

        BranchAppointmentInfo info = new BranchAppointmentInfo(branchAppointmentInfo.slotDuration(),
                branchAppointmentInfo.utilizationFactor(), branchAppointmentInfo.staffCount(),branchAppointmentInfo.dayType());

        branch.updateAppointmentInfo(branchAppointmentInfo.dayType(),info);


        try {

            log.debug("Adding branch appointment info to branch with id {},BranchAppointmentInfo:{}", branchId,branchAppointmentInfo);

           return branchAppointmentInfoService.addBranchAppointmentConfigInfo(branchAppointmentInfo.dayType(),branch);

        } catch (Exception e) {
            log.error("Unable to add branch:{} appointment info", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
   public boolean deleteBranch(String branchId) {
        boolean deleted;
        try {
            log.debug("Deleting branch with id {}", branchId);
             deleted = branchService.delete(branchId);
        } catch (Exception e) {

            log.error("Unable to delete branch with id {}", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
        return deleted;
    }
    Collection<Branch> getAllBranch() {
        Collection<Branch> branch = Collections.emptySet();
        try {
            log.debug("Retrieving all branches");
            branch = branchService.getAllBranch();
        } catch (Exception e) {
            log.error("Unable to retrieve all branches", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
        return branch;
    }


  /*  public  boolean addWorkingStaff(String branchId) {

        Set<String> staff = availableStaff.getStaff(branchId, StaffStatus.WORKING);

        //ISSUE/SEND NOTIFICATION TO ADMIN THAT ALL STAFF IS MARK AS NOT WORKING
        if (staff == null || staff.isEmpty()) {
            log.error("Unable to find staff with id {}", branchId);
            //throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found");
            //throw new StaffScheduleException("Failed to schedule staff for branch:"+branchId);
            return false;
        }

        Set<StaffRef> staffRefs = staff.stream().map(username->new StaffRef(username,"WORKING")).collect(Collectors.toSet());

        var isAdded = false;
        try {

            isAdded  =  staffSchedule.addWorkingStaff(branchId,staffRefs);

        } catch (Exception e) {
            //ISSUE/SEND NOTIFICATION TO ADMIN THAT FAILED TO ADD STAFF

            log.error("Unable to add staff:{} working", branchId, e);
            //throw new StaffScheduleException("Failed to schedule staff for branch:"+branchId);
        }
        return isAdded;
    }*/

}
