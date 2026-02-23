package capitec.branch.appointment.slots.infrastructure.controller;

import capitec.branch.appointment.slots.app.GetDailySlotsQuery;
import capitec.branch.appointment.slots.app.GetNext7DaySlotsQuery;
import capitec.branch.appointment.slots.app.GetSlotQuery;
import capitec.branch.appointment.slots.app.SlotStatusTransitionAction;
import capitec.branch.appointment.slots.app.UpdateSlotStatusUseCase;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for slot operations.
 * Provides endpoints to view, block and release slots.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
@Validated
public class SlotController {

    private final GetSlotQuery getSlotQuery;
    private final GetDailySlotsQuery getDailySlotsQuery;
    private final GetNext7DaySlotsQuery getNext7DaySlotsQuery;
    private final UpdateSlotStatusUseCase updateSlotStatusUseCase;

    /**
     * Get a slot by ID.
     *
     * @param slotId  the slot ID
     * @param traceId unique trace identifier for request tracking
     * @return the slot details
     */
    @GetMapping("/{slotId}")
    @PreAuthorize("hasAnyRole('app_user')")
    public ResponseEntity<SlotResponse> getSlot(
            @PathVariable("slotId") UUID slotId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting slot: {}, traceId: {}", slotId, traceId);

        Slot slot = getSlotQuery.execute(slotId);

        log.info("Slot retrieved: {}, traceId: {}", slotId, traceId);

        return ResponseEntity.ok(toResponse(slot));
    }

    /**
     * Get slots for a branch on a specific day.
     *
     * @param branchId the branch ID
     * @param date     the date to query
     * @param traceId  unique trace identifier for request tracking
     * @return list of slots for the day
     */
    @GetMapping("/branches/{branchId}/daily")
    @PreAuthorize("hasAnyRole('app_user')")
    public ResponseEntity<DailySlotsResponse> getDailySlots(
            @PathVariable("branchId") String branchId,
            @RequestParam("date") LocalDate date,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting daily slots for branch: {}, date: {}, traceId: {}", branchId, date, traceId);

        List<Slot> slots = getDailySlotsQuery.execute(branchId, date);

        List<SlotResponse> slotResponses = slots.stream()
                .map(this::toResponse)
                .toList();

        log.info("Found {} slots for branch: {}, date: {}, traceId: {}",
                slotResponses.size(), branchId, date, traceId);

        return ResponseEntity.ok(new DailySlotsResponse(
                branchId,
                date.toString(),
                slotResponses,
                slotResponses.size()
        ));
    }

    /**
     * Get available slots for the next 7 days.
     *
     * @param branchId the branch ID
     * @param fromDate optional start date (defaults to today)
     * @param status   optional status filter
     * @param traceId  unique trace identifier for request tracking
     * @return slots grouped by day
     */
    @GetMapping("/branches/{branchId}/week")
    @PreAuthorize("hasAnyRole('app_user')")
    public ResponseEntity<SlotsResponse> getWeeklySlots(
            @PathVariable("branchId") String branchId,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("Trace-Id") String traceId
    ) {
        LocalDate startDate = fromDate != null ? fromDate : LocalDate.now();
        log.info("Getting weekly slots for branch: {}, fromDate: {}, status: {}, traceId: {}",
                branchId, startDate, status, traceId);

        Map<LocalDate, List<Slot>> slotsByDay;

        if (status != null) {
            SlotStatus slotStatus = SlotStatus.valueOf(status.toUpperCase());
            slotsByDay = getNext7DaySlotsQuery.execute(branchId, startDate, slotStatus);
        } else {
            slotsByDay = getNext7DaySlotsQuery.execute(branchId, startDate);
        }

        Map<LocalDate, List<SlotResponse>> responseMap = slotsByDay.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(this::toResponse)
                                .toList()
                ));

        int totalCount = responseMap.values().stream()
                .mapToInt(List::size)
                .sum();

        log.info("Found {} total slots for branch: {}, traceId: {}", totalCount, branchId, traceId);

        return ResponseEntity.ok(new SlotsResponse(responseMap, totalCount));
    }

    /**
     * Block a slot to prevent bookings.
     *
     * @param slotId  the slot ID to block
     * @param traceId unique trace identifier for request tracking
     * @return the blocked slot
     */
    @PatchMapping("/{slotId}/block")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<SlotResponse> blockSlot(
            @PathVariable("slotId") UUID slotId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Blocking slot: {}, traceId: {}", slotId, traceId);

        var action = new SlotStatusTransitionAction.Block(slotId, LocalDateTime.now());
        updateSlotStatusUseCase.execute(action);

        Slot slot = getSlotQuery.execute(slotId);

        log.info("Slot blocked successfully: {}, traceId: {}", slotId, traceId);

        return ResponseEntity.ok(toResponse(slot));
    }

    /**
     * Release a blocked slot to allow bookings.
     *
     * @param slotId  the slot ID to release
     * @param traceId unique trace identifier for request tracking
     * @return the released slot
     */
    @PatchMapping("/{slotId}/release")
    @PreAuthorize("hasAnyRole('app_staff')")
    public ResponseEntity<SlotResponse> releaseSlot(
            @PathVariable("slotId") UUID slotId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Releasing slot: {}, traceId: {}", slotId, traceId);

        var action = new SlotStatusTransitionAction.Release(slotId, LocalDateTime.now());
        updateSlotStatusUseCase.execute(action);

        Slot slot = getSlotQuery.execute(slotId);

        log.info("Slot released successfully: {}, traceId: {}", slotId, traceId);

        return ResponseEntity.ok(toResponse(slot));
    }

    private SlotResponse toResponse(Slot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getDay(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getMaxBookingCapacity(),
                slot.getBookingCount(),
                slot.getBranchId(),
                slot.getStatus().name()
        );
    }
}
