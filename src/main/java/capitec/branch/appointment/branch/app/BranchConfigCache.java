package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class BranchConfigCache {

    private final Map<String, Branch> cache = new ConcurrentHashMap<>();
    private volatile boolean warmed = false;
    private final BranchService branchService;

    // Optional: Background warmup after startup
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmCache() {
        if (!warmed) {
            int batchSize = 100;
            int page = 0;
            Collection<Branch> batch;

            do {
                batch = branchService.getAllBranch(page++, batchSize);
                batch.forEach(branch -> cache.put(branch.getBranchId(), branch));
            } while (!batch.isEmpty());

            warmed = true;
        }
    }
}
