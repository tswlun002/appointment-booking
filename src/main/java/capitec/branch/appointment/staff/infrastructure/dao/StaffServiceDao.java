package capitec.branch.appointment.staff.infrastructure.dao;

import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
class StaffServiceDao implements StaffService {

    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;

    @Override
    public boolean addStaff(@Valid Staff staff) {
        var isAdded  = false;

        try {

            log.debug("Adding staff: {}", staff);

            StaffEntity entity = staffMapper.toEntity(staff);

            staffRepository.save(entity);
            isAdded = true;

        } catch (Exception e) {

            log.error( "Error adding staff to database. {}", e );
            throw e;
        }

        return isAdded;
    }

    @Override
    public Optional<Staff> updateStaff(String username, StaffStatus status) {

        Optional<StaffEntity> updated;
        try {

            log.debug("Updating staff:{} status: {}",username, status);

            updated = staffRepository.updatedStaffStatus(username,status.name());

        } catch (Exception e) {
            log.error( "Error updating staff:{} status:{} to database.",username,status, e );
            throw e;
        }
        return updated.map(staffMapper::toDomain);
    }

    @Override
    public Set<Staff> getStaffByBranchAndStatus(String branchId, StaffStatus status) {

        Set<StaffEntity> staffEntities;
        try {
            log.debug("Getting staff by branch:{} and status: {}",branchId,status);
            staffEntities = staffRepository.getStaffByBranchIdAndStatus(branchId, status.name());

        } catch (Exception e) {

            log.error( "Error getting branch:{} staff with status:{} from database.",branchId,status, e );
            throw e;
        }

        return staffEntities.stream().map(staffMapper::toDomain).collect(Collectors.toSet());
    }
}

