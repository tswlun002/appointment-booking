package capitec.branch.appointment.branch.infrastructure.dao;

import capitec.branch.appointment.branch.app.port.BranchQueryPort;
import capitec.branch.appointment.branch.app.port.BranchQueryResult;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfoService;
import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverrideService;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Validated
public class BranchDaoImpl implements BranchService, BranchQueryPort, BranchAppointmentInfoService, OperationHoursOverrideService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
    private final CacheManager cacheManager;
    public static final String CACHE_NAME = "branches";
    public static final String CACHE_MANAGER_NAME = "branchCacheManager";

    public BranchDaoImpl( BranchRepository branchRepository, BranchMapper branchMapper,
                          @Qualifier(value = CACHE_MANAGER_NAME) CacheManager cacheManager) {
        this.branchRepository = branchRepository;
        this.branchMapper = branchMapper;
        this.cacheManager = cacheManager;
    }

    @Override
    @CachePut(value = CACHE_NAME,cacheManager = CACHE_MANAGER_NAME, key = "#branch.getBranchId()")
    public Branch add(@Valid Branch branch) {

        BranchEntity entity = branchMapper.toEntity(branch);

        try {

            log.debug("Adding branch: {}", branch);

           entity= branchRepository.save(entity);

        } catch (Exception e) {

            log.error("Unable to save branch.", e);
            if(e instanceof DuplicateKeyException ||( e.getCause() !=null && e.getCause() instanceof DuplicateKeyException )) {

                throw new EntityAlreadyExistException(e.getMessage());
            }
            throw e;
        }
        return BranchMapper.toDomain(entity);

    }

    // ==================== BranchQueryPort Implementation ====================

    @Override
    @Cacheable(value = CACHE_NAME,cacheManager = CACHE_MANAGER_NAME, key = "#branchId", unless = "#result == null")
    public Optional<Branch> findByBranchId(String branchId) {

        Optional<Branch> branch;
        try {

            Optional<BranchEntity> branchById = branchRepository.getByBranchId(branchId);
            branch = branchById.map(BranchMapper::toDomain);

        } catch (Exception e) {

            log.error("Unable to get branch:{}",branchId, e);
            throw e;
        }
        return branch;
    }

    @Override
    @CacheEvict(value = CACHE_NAME,cacheManager = CACHE_MANAGER_NAME, key = "#branchId",condition = "#result == true")
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
    public BranchQueryResult findAll(int offset, int limit) {

        Collection<BranchEntity> branchEntities = branchRepository.getAllBranch(offset, limit);

        // Extract total count from first entity (all entities have same count due to window function)
        int totalCount = branchEntities.stream()
                .findFirst()
                .map(BranchEntity::totalCount)
                .map(Long::intValue)
                .orElse(0);

        List<Branch> branches = branchEntities.stream()
                .map(BranchMapper::toDomain)
                .toList();

        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            branches.forEach(branch -> cache.putIfAbsent(branch.getBranchId(), branch));
        }

        return BranchQueryResult.of(branches, totalCount);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmCache() {

        log.info("Start warming branch cache");
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return;

        int offset = 0;
        int limit = 100;
        Collection<Branch> batch;
        do {
            batch = branchRepository.getAllBranch(offset, limit)
                    .stream().map(BranchMapper::toDomain).toList();

            batch.forEach(branch -> cache.put(branch.getBranchId(), branch));
            offset += limit;

        } while (!batch.isEmpty());

        log.info("Branch cache warmed");
    }


    @Override
    @CacheEvict(value = CACHE_NAME, cacheManager = CACHE_MANAGER_NAME, key = "#branch.getBranchId()",condition = "#result == true")
    public boolean addBranchAppointmentConfigInfo(@NotNull DayType day, @Valid Branch branch) {

        var branchAppointmentInfo = branch.getBranchAppointmentInfo()
                .stream()
                .filter(info -> info.day().equals(day))
                .findFirst()
                .orElseThrow(() -> {
                            log.error("No appointment info found for day: {}", day);
                            return new NotFoundException("No appointment info found for day : " + day);
                });

        var isAdded = false;

        try {

            var added = branchRepository.addBranchAppointmentConfigInfo(
                    branch.getBranchId(), (int) branchAppointmentInfo.slotDuration().toMinutes(),
                    branchAppointmentInfo.utilizationFactor(),branchAppointmentInfo.staffCount(),
                    branchAppointmentInfo.day(), branchAppointmentInfo.maxBookingCapacity());

            isAdded = added == 1;

        } catch (Exception e) {

            log.error("Unable to add branch:{} appointment config information.",branch.getBranchId(), e);
            throw e;
        }
        return isAdded;
    }

    @Override
    @CacheEvict(value = CACHE_NAME,cacheManager = CACHE_MANAGER_NAME, key = "#branch.getBranchId()",condition = "#result == true")
    public boolean addBranchOperationHoursOverride(@NotNull LocalDate day, @Valid Branch branch) {

        var operationHoursOverride = branch.getOperationHoursOverride()
                .stream()
                .filter(operation -> operation.effectiveDate().equals(day))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No operation hours override found for day: {}", day);
                    return new NotFoundException("No operation hours override found for day: " + day);
                });

        var isAdded = false;

        try {

            var added = branchRepository.addBranchOperationHoursOverride(
                    branch.getBranchId(),
                    operationHoursOverride.effectiveDate(),
                    operationHoursOverride.openAt(),
                    operationHoursOverride.closeAt(),
                    operationHoursOverride.closed(),
                    operationHoursOverride.reason()
            );

            isAdded = added == 1;

        } catch (Exception e) {

            log.error("Unable to add branch:{} operation hours override.",branch.getBranchId(), e);
            throw e;
        }
        return isAdded;
    }

}
