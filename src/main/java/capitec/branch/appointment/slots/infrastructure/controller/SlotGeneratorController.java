package capitec.branch.appointment.slots.infrastructure.controller;

import capitec.branch.appointment.slots.app.SlotGeneratorScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for admin slot generation operations.
 * Provides endpoints to manually trigger slot generation.
 */
@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
@Slf4j
public class SlotGeneratorController {

    private final SlotGeneratorScheduler slotGeneratorScheduler;

    /**
     * Manually trigger slot generation.
     *
     * @param traceId unique trace identifier for request tracking
     * @return success message
     */
    @PostMapping("/generate")
    public ResponseEntity<String> triggerSlotGeneration(
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Manual slot generation triggered by admin, traceId: {}", traceId);
        try {
            slotGeneratorScheduler.executeWithRetry();
            log.info("Slot generation completed successfully, traceId: {}", traceId);
            return ResponseEntity.ok("Slot generation completed successfully");
        } catch (Exception e) {
            log.error("Manual slot generation failed, traceId: {}", traceId, e);
            return ResponseEntity.internalServerError()
                    .body("Slot generation failed: " + e.getMessage());
        }
    }
}
