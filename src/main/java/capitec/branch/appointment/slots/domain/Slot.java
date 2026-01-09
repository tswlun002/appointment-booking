package capitec.branch.appointment.slots.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public class Slot {

    public static String ID_FIELD_NAME = "id";
    public static String STATUS_FIELD_NAME = "status";
    public static String VERSION_FIELD_NAME = "version";

    @NotNull
    private final UUID id;
    @NotNull(message = "Day cannot be null")
    private final LocalDate day;

    @NotNull(message = "Start time cannot be null")
    private final LocalTime startTime;

    @NotNull(message = "End time cannot be null")
    private final LocalTime endTime;

    @NotNull(message = "Max capacity cannot be null")
    @Positive(message = "Max capacity must be positive")
    private final Integer maxBookingCapacity;

    @NotNull(message = "Booking count cannot be null")
    @PositiveOrZero(message = "Booking count must be zero or positive")
    private Integer bookingCount;

    @NotBlank(message = "Branch ID cannot be blank")
    private final String branchId;

    @NotNull(message = "Status cannot be null")
    private SlotStatus status;

    private int version;

    public Slot(LocalDate day, LocalTime startTime, LocalTime endTime, Integer maxBookingCapacity, String branchId) {
        this.id = UUID.randomUUID();
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxBookingCapacity = maxBookingCapacity;
        this.bookingCount = 0;
        this.branchId = branchId;
        this.status = SlotStatus.AVAILABLE;
        this.version = 0;

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Slot start time (" + startTime + ") must be strictly before the end time (" + endTime + ").");
        }
        if (maxBookingCapacity <= 0) {
            throw new IllegalArgumentException("Max capacity must be positive.");
        }
    }

    public void book(LocalDateTime currentTime) {
        validateSlotTimeNotPassed(currentTime);
        if (status == SlotStatus.BLOCKED) {
            throw new IllegalStateException("Cannot book a blocked slot.");
        }
        if (status == SlotStatus.EXPIRED) {
            throw new IllegalStateException("Cannot book an expired slot.");
        }
        if (bookingCount >= maxBookingCapacity) {
            throw new IllegalStateException("Slot is fully booked.");
        }

        this.bookingCount++;
        if (bookingCount.equals(maxBookingCapacity)) {
            this.status = SlotStatus.BOOKED;
            //this.version++;  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking

        }
    }

    public void release(LocalDateTime currentTime) {
        validateSlotTimeNotPassed(currentTime);
        if (bookingCount <= 0) {
            throw new IllegalStateException("No bookings to release.");
        }

        this.bookingCount--;
        if (status == SlotStatus.BOOKED && bookingCount < maxBookingCapacity) {
            this.status = SlotStatus.AVAILABLE;
            //this.version++;  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking

        }
        // If BLOCKED, remain BLOCKED per business rule
    }

    public void expire() {
        if (status == SlotStatus.AVAILABLE || status == SlotStatus.BLOCKED) {
            this.status = SlotStatus.EXPIRED;
            //this.version++;  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking

        }
    }

    public void block(LocalDateTime currentTime) {
        validateSlotTimeNotPassed(currentTime);
        if (status == SlotStatus.BLOCKED) {
            throw new IllegalStateException("Slot is already blocked.");
        }
        if (status == SlotStatus.EXPIRED) {
            throw new IllegalStateException("Cannot block an expired slot.");
        }
        this.status = SlotStatus.BLOCKED;
        //this.version++;  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking

    }

    public void unblock(LocalDateTime currentTime) {
        validateSlotTimeNotPassed(currentTime);
        if (status != SlotStatus.BLOCKED) {
            throw new IllegalStateException("Slot is not blocked.");
        }
        this.status = bookingCount < maxBookingCapacity ? SlotStatus.AVAILABLE : SlotStatus.BOOKED;
        //this.version++;  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking
    }

    public boolean hasAvailableCapacity() {
        return status == SlotStatus.AVAILABLE && bookingCount < maxBookingCapacity;
    }

    private void validateSlotTimeNotPassed(LocalDateTime currentTime) {
        LocalDateTime slotDateTime = LocalDateTime.of(day, startTime);
        if (currentTime.isAfter(slotDateTime)) {
            throw new IllegalStateException("Cannot modify a slot that has already started.");
        }
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDay() {
        return day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    public Integer getMaxBookingCapacity() {
        return maxBookingCapacity;
    }

    public Integer getBookingCount() {
        return bookingCount;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public String getBranchId() {
        return branchId;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Slot slot)) return false;
        return Objects.equals(id, slot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Slot{" +
                "id=" + id +
                ", day=" + day +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", maxCapacity=" + maxBookingCapacity +
                ", bookingCount=" + bookingCount +
                ", branchId='" + branchId + '\'' +
                ", status=" + status +
                ", version=" + version +
                '}';
    }
}
