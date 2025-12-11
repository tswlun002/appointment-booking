package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;

import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfoService;
import capitec.branch.appointment.day.domain.DayType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class BranchDaoImpl implements BranchService, BranchAppointmentInfoService/*StaffSchedule*/ {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Override
    public Branch add(@Valid Branch branch) {

        BranchEntity entity = branchMapper.toEntity(branch);

        try {

            log.debug("Adding branch: {}", branch);

           entity= branchRepository.save(entity);

        } catch (Exception e) {

            log.error("Unable to save branch.", e);
            throw e;
        }
        return branchMapper.toDomain(entity);

    }

    @Override
    public Optional<Branch> getByBranchId(String branchId) {

        Optional<Branch> branch;
        try {

            Optional<BranchEntity> branchById = branchRepository.getByBranchId(branchId);
            branch = branchById.map(branchMapper::toDomain);

        } catch (Exception e) {

            log.error("Unable to get branch:{}",branchId, e);
            throw e;
        }
        return branch;
    }

    @Override
    public boolean delete(String branchId) {
        var isDeleted = false;

        try {

            isDeleted =branchRepository.deletBranch(branchId)==1;

        } catch (Exception e) {
            log.error("Unable to delete branch:{}",branchId, e);
            throw e;
        }
        return isDeleted;
    }

    @Override
    public Collection<Branch> getAllBranch() {
        Collection<BranchEntity> branchEntities= branchRepository.getAllBranch();
        return branchEntities.stream().map(branchMapper::toDomain).collect(Collectors.toSet());
    }


    @Override
    public boolean addBranchAppointmentConfigInfo(@NotNull DayType dayType,  @Valid Branch branch) {

        var branchAppointmentInfo = branch.getBranchAppointmentInfo()
                .stream()
                .filter(info -> info.dayType().equals(dayType))
                .findFirst()
                .orElseThrow(() -> {
                            log.error("No appointment info found for day type: {}", dayType.name());
                            return new NotFoundException("No appointment info found for day type: " + dayType.name());
                });

        var isAdded = false;

        try {

            var added = branchRepository.addBranchAppointmentConfigInfo(branch.getBranchId(), (int) branchAppointmentInfo.slotDuration().toMinutes(),
                    branchAppointmentInfo.utilizationFactor(),branchAppointmentInfo.staffCount(), branchAppointmentInfo.dayType().name());

            isAdded = added == 1;

        } catch (Exception e) {

            log.error("Unable to add branch:{} appointment config information.",branch.getBranchId(), e);
            throw e;
        }
        return isAdded;
    }

 /*   @Override
    public boolean addWorkingStaff(String branchId, Set<StaffRef> staff) {

      var isAdded = false;
        try {
            isAdded=branchRepository.addWorkingStaff(branchId,staff);
        } catch (Exception e) {
            log.error("Unable to add branch:{} working staff", branchId,e);
        }
        return isAdded;
    }*/
}
