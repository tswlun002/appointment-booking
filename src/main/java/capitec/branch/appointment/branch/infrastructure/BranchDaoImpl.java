package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfoService;
import capitec.branch.appointment.day.domain.DayType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class BranchDaoImpl implements BranchService, BranchAppointmentInfoService {

    private final BranchRepository branchRepository;
    private final BranchMapper  branchMapper;

    @Override
    @Transactional
    public void add(@Valid Branch branch) {

        BranchEntity entity = branchMapper.toEntity(branch);

        try{

             log.debug("Adding branch: {}", branch);

             branchRepository.save(entity);

        } catch (Exception e) {

            log.error("Unable to save branch.", e);
            throw e;
        }

    }

    @Override
    public Optional<Branch> getByBranchId(String branchId) {

        Optional<Branch> branch;
        try {

            Optional<BranchEntity> branchById = branchRepository.getBranchById(branchId);
            branch = branchById.map(branchMapper::toDomain);

        } catch (Exception e) {

            log.error("Unable to get branch.", e);
            throw e;
        }
        return branch;
    }

    @Transactional
    @Override
    public boolean addBranchAppointmentConfigInfo(@NotNull DayType dayType, @Valid Branch branch) {

       var branchAppointmentInfo = branch.getBranchAppointmentInfo().get(dayType);
         var isAdded = false;
        try{

          var added =  branchRepository.addBranchAppointmentConfigInfo(branch.getBranchId(), (int) branchAppointmentInfo.slotDuration().toMinutes(),
                  branchAppointmentInfo.utilizationFactor(),branchAppointmentInfo.dayType().name());

          isAdded= added==1;

        } catch (Exception e) {

            log.error("Unable to save branch.", e);
            throw e;
        }
        return isAdded;
    }
}
