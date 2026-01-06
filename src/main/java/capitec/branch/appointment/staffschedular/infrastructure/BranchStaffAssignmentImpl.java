package capitec.branch.appointment.staffschedular.infrastructure;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignment;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignmentService;
import capitec.branch.appointment.staffschedular.domain.StaffRef;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Validated
class BranchStaffAssignmentImpl implements BranchStaffAssignmentService {

    private final BranchStaffAssignmentRepository repository;
    private final BranchStaffAssignmentMapper mapper;

    @Override
    @Transactional
    public boolean addStaff(@Valid  BranchStaffAssignment branchStaffAssignment) {
        var isAdded = false;
        try {

            Set<BranchStaffAssignmentEntity> staff = mapper.toBranchStaffAssignmentEntity(branchStaffAssignment);
            repository.bulkUpsertAssignments(staff);
            isAdded = true;
        }
        catch (DuplicateKeyException e) {

            log.error("Failed to save branch staff assignment.\n", e);
            throw new EntityAlreadyExistException(e.getMessage());
        }
        catch (Exception e) {

            log.error("Failed to save branch staff assignment.\n", e);
            throw e;
        }

        return isAdded;


    }


    @Override
    public Optional<BranchStaffAssignment> get(String branchId) {

        try {

            log.debug("Getting branch staff assignment {}", branchId);

           return get(branchId,null);

        } catch (Exception e) {

            log.error("Failed to save branch staff assignment {}", branchId, e);
            throw e;
        }

    }

    @Override
    public Optional<BranchStaffAssignment> get(String branchId, LocalDate day) {
        try {

            log.debug("Getting branch staff assignment {} at day {}", branchId, day);


            Set<BranchStaffAssignmentEntity> staff = repository.getWorkingStaff(branchId, day);

            return staff.isEmpty() ? Optional.empty() : Optional.of(mapper.toDomain(staff, branchId));

        } catch (Exception e) {

            log.error("Failed to save branch staff assignment {}", branchId, e);
            throw e;
        }
    }

    @Override
    public Set<StaffRef>  getWorkingStaff(String branchId) {

       Set<BranchStaffAssignmentEntity> staff = repository.getWorkingStaff(branchId,null);


        return staff.stream().map(BranchStaffAssignmentEntity::username)
                    .map(StaffRef::new).collect(Collectors.toSet());

    }

    @Override
    public Set<StaffRef> getWorkingStaff(String branchId, LocalDate date) {
        Set<BranchStaffAssignmentEntity> staff = repository.getWorkingStaff(branchId,date);

        Set<StaffRef> collect;
        try {
            collect = staff.stream().map(BranchStaffAssignmentEntity::username)
                    .map(StaffRef::new).collect(Collectors.toSet());

        } catch (Exception e) {

            log.error("Failed to save branch staff assignment {}", branchId, e);
            throw e;
        }

        return collect;
    }

    @Override
    public boolean cancelWorkingDay(String branchId, Set<LocalDate> date) {
        try{

            int affectedRows = repository.cancelWorkingDay(branchId, date);
            return affectedRows >0;
        }
        catch (Exception e) {
          log.error("Failed to cancel working day {} of branch {}", date, branchId, e);
          throw e;
        }
    }

//    @Override
//    public boolean addStaff(String branchId, String username, LocalDate day) {
//        var isAdded = false;
//
//        try {
//
//            isAdded =repository.addStaff( branchId, day,  username)==1;
//
//        } catch (Exception e) {
//
//            log.error("Failed to save branch staff assignment {}", branchId, e);
//            throw e;
//        }
//        return isAdded;
//    }
}
