package capitec.branch.appointment.slots.domain;

import capitec.branch.appointment.exeption.SlotFullyBookedException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public class Slot {
    private static Logger log = LoggerFactory.getLogger(Slot.class);

    @NotNull
    private final UUID id;
    @NotNull(message = "Day cannot be null")
    private final LocalDate day;

    @NotNull(message = "Start time cannot be null")
    private final LocalTime startTime;

    @NotNull(message = "End time cannot be null")
    private final LocalTime endTime;

    @NotNull(message = "Max booking capacity cannot be null")
    @Positive(message = "Max booking capacity must be greater than zero")
    private final Integer maxBookingCapacity;

    @NotNull(message = "Booking count cannot be null")
    @PositiveOrZero(message = "Booking count must be zero or positive")
    private Integer bookingCount;

    @NotBlank(message = "Branch ID cannot be blank")
    private final String branchId;

    @NotNull(message = "Status cannot be null")
    private SlotStatus status;

    private int version;

    // Can add CreatedAt and Updated . They will be used to check/verify if the booking at book slot can be honoured or not
    //But now we still trust code base, we assume all booking done were before slot is block so we honour them without verifying

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

        validateCreationInvariants();
    }
    private Slot(
            UUID id, LocalDate day, LocalTime startTime, LocalTime endTime,
            Integer maxBookingCapacity, Integer bookingCount, String branchId,
            SlotStatus status, int version) {

        this.id = Objects.requireNonNull(id, "ID must not be null during reconstitution.");
        this.day = Objects.requireNonNull(day, "Day must not be null during reconstitution.");
        this.startTime = Objects.requireNonNull(startTime, "Start time must not be null during reconstitution.");
        this.endTime = Objects.requireNonNull(endTime, "End time must not be null during reconstitution.");
        this.maxBookingCapacity = Objects.requireNonNull(maxBookingCapacity, "Max capacity must not be null during reconstitution.");
        this.bookingCount = Objects.requireNonNull(bookingCount, "Booking count must not be null during reconstitution.");
        this.branchId = Objects.requireNonNull(branchId, "Branch ID must not be null during reconstitution.");
        this.status = Objects.requireNonNull(status, "Status must not be null during reconstitution.");
        this.version = version;

        // Validation for consistency of loaded data
        validateExistingInvariants();
    }
    public static Slot reconstituteFromPersistence(
            UUID id, LocalDate day, LocalTime startTime, LocalTime endTime,
            Integer maxBookingCapacity, Integer bookingCount, String branchId,
            SlotStatus status, int version) {

        Assert.notNull(id, "Cannot reconstitute Slot: ID must not be null.");
        Assert.notNull(day, "Cannot reconstitute Slot: Day must not be null.");
        Assert.notNull(startTime, "Cannot reconstitute Slot: Start time must not be null.");
        Assert.notNull(endTime, "Cannot reconstitute Slot: End time must not be null.");
        Assert.notNull(maxBookingCapacity, "Cannot reconstitute Slot: Max capacity must not be null.");
        Assert.notNull(bookingCount, "Cannot reconstitute Slot: Booking count must not be null.");

        // Use hasText for String checks (handles null and blank)
        Assert.hasText(branchId, "Cannot reconstitute Slot: Branch ID must not be null or blank.");

        Assert.notNull(status, "Cannot reconstitute Slot: Status must not be null.");

        return new Slot(
                id, day, startTime, endTime,
                maxBookingCapacity, bookingCount, branchId,
                status, version);
    }


    public void  book(LocalDateTime currentTime) {
        validateSlotTimeNotPassed(currentTime);
        if (status == SlotStatus.BLOCKED) {
            throw new IllegalStateException("Cannot book a blocked slot.");
        }
        if (status == SlotStatus.EXPIRED) {
            throw new IllegalStateException("Cannot book an expired slot.");
        }
        if (!hasAvailableCapacity()) {
            throw new SlotFullyBookedException("Slot is fully booked.");
        }

        this.bookingCount++;
        if (bookingCount.equals(maxBookingCapacity)) {
            this.status = SlotStatus.FULLY_BOOKED;
            //this.version++;  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking

        }
    }


    /**
     * Release booked slot
     * Only slot that has start time is passed yet that can be released
     * Booking count is decremented when slot is released
     * Make slot available if slot is not blocked, is fully booked and
     * booking count still less than maximum booking capacity
     * @param currentTime is the dateOfSlots time when release slot is performed
     * @throws  IllegalStateException when try to release slot that not yet booked
     */
    public void release(LocalDateTime currentTime) {
        validateSlotTimeNotPassed(currentTime);
        if (bookingCount == 0) {
            throw new IllegalStateException("No bookings to release.");
        }

        this.bookingCount--;
        if ( status == SlotStatus.FULLY_BOOKED && bookingCount < maxBookingCapacity) {
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

    public synchronized void  block(LocalDateTime currentTime) {
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
        this.status = bookingCount < maxBookingCapacity ? SlotStatus.AVAILABLE : SlotStatus.FULLY_BOOKED;
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
    private void validateCreationInvariants() {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Slot start time (" + startTime + ") must be strictly before the end time (" + endTime + ").");
        }
        if (maxBookingCapacity == null || maxBookingCapacity <= 0) {
            throw new IllegalArgumentException("Max booking capacity must be greater than zero.");
        }
        // bookingCount is 0, so no need to check < 0
    }
    private void validateExistingInvariants() {

        // VITAL: Booking count must never exceed max capacity
        if (bookingCount > maxBookingCapacity) {
            throw new IllegalStateException("DATA CORRUPTION: bookingCount (" + bookingCount + ") exceeds maxBookingCapacity (" + maxBookingCapacity + ") for Slot ID " + id + ".");
        }

        // VITAL: Status consistency check (Strictly enforce business rules on loaded data)
        if (status == SlotStatus.FULLY_BOOKED && bookingCount < maxBookingCapacity) {
            throw new IllegalStateException("DATA CORRUPTION: Slot ID " + id + " has status BOOKED but is not full (Count: " + bookingCount + ", Max: " + maxBookingCapacity + ").");
        }

        if (status == SlotStatus.AVAILABLE && bookingCount >= maxBookingCapacity) {
            throw new IllegalStateException("DATA CORRUPTION: Slot ID " + id + " has status AVAILABLE but is full (Count: " + bookingCount + ").");
        }

        // Note: SlotStatus.BLOCKED and SlotStatus.EXPIRED allow any bookingCount (>=0 and <= maxCapacity)
        // so no specific consistency check is needed for those states here.
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
