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

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class UpdateBranchAppointmentConfigUseCase {

    private final BranchService branchService;
    private final BranchAppointmentInfoService branchAppointmentInfoService;

    public boolean execute(String branchId, @Valid BranchAppointmentInfoDTO branchAppointmentInfo) {

        // 1. Fetch the branch (or throw NOT_FOUND)
        Branch branch = branchService.getByBranchId(branchId).orElseThrow(() ->{
            log.error("Unable to find branch with id {}", branchId);
           return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
        });

        // 2. Create the domain object
        BranchAppointmentInfo info = new BranchAppointmentInfo(
            branchAppointmentInfo.slotDuration(),
            branchAppointmentInfo.utilizationFactor(), 
            branchAppointmentInfo.staffCount(),
            branchAppointmentInfo.dayType()
        );

        // 3. Apply changes to the aggregate root
        branch.updateAppointmentInfo(branchAppointmentInfo.dayType(), info);

        try {
            log.debug("Adding branch appointment info to branch with id {}, BranchAppointmentInfo:{}", branchId, branchAppointmentInfo);
            // 4. Persist the updated state
            return branchAppointmentInfoService.addBranchAppointmentConfigInfo(branchAppointmentInfo.dayType(), branch);

        } catch (Exception e) {
            log.error("Unable to update branch:{} appointment info", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error during config update");
        }
    }
}