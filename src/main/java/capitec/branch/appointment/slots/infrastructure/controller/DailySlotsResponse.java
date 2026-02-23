package capitec.branch.appointment.slots.infrastructure.controller;

import java.util.List;

public record DailySlotsResponse(
        String branchId,
        String date,
        List<SlotResponse> slots,
        int totalCount
) {}
