package capitec.branch.appointment.slots.app;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class SlotGeneratorSchedular {

    private final  GenerateSlotsUseCase  GenerateSlotsUseCase;
    private final GetLastestGeneratedSlotDate  getLastestGeneratedSlotDate;
    @Value("${slot.rolling-window-days.init-value:7}")
    private int initRollingWindowDays;
    @Value("${slot.rolling-window-days.daily-value:1}")
    private int dailyRollingWindowDays;



    /**
     * Scheduled task that runs every night at 00:30 (12:30 AM) by default
     * Generates slots for  days ahead using a rolling window approach
     */
    @Scheduled(cron = "${slot.cron:0 30 0 * * *}", zone = "Africa/Johannesburg")
    public void generateDailySlots() {

        LocalDateTime now = LocalDateTime.now();
        log.info("Starting daily slot generation at {}", now);

        try {

            var latestDate = getLastestGeneratedSlotDate.execute(now.toLocalDate());

            if(latestDate.isPresent()){
                GenerateSlotsUseCase.createNext7DaySlots(latestDate.get().plusDays(1), dailyRollingWindowDays);
            }
            else {
                GenerateSlotsUseCase.createNext7DaySlots(LocalDate.now().plusDays(1), initRollingWindowDays);
            }

            log.info("Successfully completed daily slot generation at {}", now);
        } catch (Exception e) {
            log.error("Error during daily slot generation: {}", e.getMessage(), e);
            // Consider adding alerting/monitoring here for production
            throw new RuntimeException("Error during daily slot generation: " + e.getMessage(), e);
        }
    }
}
