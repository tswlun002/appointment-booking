package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.day.domain.DayType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;


@ConfigurationProperties(prefix = "slot-properties")
public record MapSlotProperties(
     Map<DayType, SlotProperties> slotProperties
) {
}
