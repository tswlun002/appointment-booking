package capitec.branch.appointment.slots.infrastructure.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record SlotsResponse(
        Map<LocalDate, List<SlotResponse>> slotsByDay,
        int totalCount
) {}
