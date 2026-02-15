package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.app.port.BranchQueryPort;
import capitec.branch.appointment.branch.app.port.BranchQueryResult;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class BranchConfigCache {

    private final Map<String, Branch> cache = new ConcurrentHashMap<>();
    private volatile boolean warmed = false;
    private final BranchQueryPort branchQueryPort;

    // Optional: Background warmup after startup
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmCache() {
        if (!warmed) {
            int batchSize = 100;
            int offset = 0;
            List<Branch> batch;

            do {
                BranchQueryResult result = branchQueryPort.findAll(offset, batchSize);
                batch = result.branches();
                batch.forEach(branch -> cache.put(branch.getBranchId(), branch));
                offset += batchSize;
            } while (!batch.isEmpty());

            warmed = true;
        }
    }
}
