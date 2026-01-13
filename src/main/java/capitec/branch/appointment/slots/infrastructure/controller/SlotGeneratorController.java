package capitec.branch.appointment.slots.infrastructure.controller;

import capitec.branch.appointment.slots.app.SlotGeneratorScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/slots")
@RequiredArgsConstructor
@Slf4j
public class SlotGeneratorController {

    private final SlotGeneratorScheduler slotGeneratorScheduler;

    @PostMapping("/generate")
    public ResponseEntity<String> triggerSlotGeneration() {
        log.info("Manual slot generation triggered by admin");
        try {
            slotGeneratorScheduler.executeWithRetry();
            return ResponseEntity.ok("Slot generation completed successfully");
        } catch (Exception e) {
            log.error("Manual slot generation failed", e);
            return ResponseEntity.internalServerError()
                    .body("Slot generation failed: " + e.getMessage());
        }
    }
}
