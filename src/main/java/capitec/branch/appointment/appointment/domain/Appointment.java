package capitec.branch.appointment.appointment.domain;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Appointment {

    // --- Validation Constants ---
    private static final int MIN_BOOKING_ADVANCE_MINUTES = 60;
    private static final int CANCELLATION_WINDOW_MINUTES = 120;
    private static final int GRACE_WINDOW_MINUTES = 5;
    private static final int MAX_RESCHEDULE_COUNT = 3;
    private static final String BOOKING_REF_PREFIX = "APT";
    private static final int BOOKING_REF_YEAR_LENGTH = 4;
    private static final int BOOKING_REF_SEQUENCE_LENGTH = 7;

    // --- Identity and Foreign Keys ---
    @NotNull(message = "Appointment ID cannot be null")
    private final UUID id;

    @NotNull(message = "Slot ID cannot be null")
    private UUID slotId;

    @NotBlank(message = "Branch ID cannot be blank")
    private final String branchId;

    @Username
    private final String customerUsername;

    @NotBlank(message = "Service type cannot be blank")
    private final String serviceType;

    // --- State Management ---
    @NotNull(message = "Appointment status cannot be null")
    private AppointmentStatus status;

    @NotBlank(message = "Booking reference cannot be blank")
    @Pattern(regexp = "^APT-\\d{4}-\\d{7}$",
            message = "Booking reference must match pattern: APT-YYYY-XXXXXXX")
    private String bookingReference;

    @Min(value = 0, message = "Version cannot be negative")
    @Max(value = Integer.MAX_VALUE, message = "Version exceeds maximum allowed value")
    private int version;

    // --- Timestamps ---
    @NotNull(message = "Created timestamp cannot be null")
    @PastOrPresent(message = "Created timestamp cannot be in the future")
    private final LocalDateTime createdAt;

    @NotNull(message = "Updated timestamp cannot be null")
    @PastOrPresent(message = "Updated timestamp cannot be in the future")
    private LocalDateTime updatedAt;

    @PastOrPresent(message = "Checked-in timestamp cannot be in the future")
    private LocalDateTime checkedInAt;

    @PastOrPresent(message = "In-progress timestamp cannot be in the future")
    private LocalDateTime inProgressAt;

    @PastOrPresent(message = "Completed timestamp cannot be in the future")
    private LocalDateTime completedAt;

    @PastOrPresent(message = "Terminated timestamp cannot be in the future")
    private LocalDateTime terminatedAt;

    // --- Audit Trail ---
    @Size(min = 2, max = 50, message = "Terminated by ID must be between 2 and 50 characters")
    private String terminatedBy;

    private AppointmentTerminationReason terminationReason;

    @Size(max = 500, message = "Termination notes cannot exceed 500 characters")
    private String terminationNotes;

    // --- Service Execution ---
    @Size(min = 2, max = 50, message = "Consultant ID must be between 2 and 50 characters")
    private String assignedConsultantId;

    @Size(max = 1000, message = "Service notes cannot exceed 1000 characters")
    private String serviceNotes;

    // --- Rescheduling Support ---
    private UUID previousSlotId;

    @Min(value = 0, message = "Reschedule count cannot be negative")
    @Max(value = MAX_RESCHEDULE_COUNT, message = "Reschedule count cannot exceed " + MAX_RESCHEDULE_COUNT)
    private int rescheduleCount;

    // --- Constructor for new booking ---
    private Appointment(UUID slotId, String branchId, String customerUsername, String serviceType) {
        validateConstructorInputs(slotId, branchId, customerUsername, serviceType);

        this.id = UUID.randomUUID();
        this.slotId = slotId;
        this.branchId = branchId;
        this.customerUsername = customerUsername;
        this.serviceType = serviceType;
        this.status = AppointmentStatus.BOOKED;
        this.bookingReference = generateBookingReference();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.version = 0;
        this.rescheduleCount = 0;
    }

    // --- Input Validation ---

    private static void validateConstructorInputs(UUID slotId, String branchId, String customerId, String serviceType) {
        if (slotId == null) {
            throw new IllegalArgumentException("Slot ID cannot be null");
        }
        if (branchId == null || branchId.isBlank()) {
            throw new IllegalArgumentException("Branch ID cannot be null or blank");
        }
        if (branchId.length() < 2 || branchId.length() > 50) {
            throw new IllegalArgumentException("Branch ID must be between 2 and 50 characters");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (customerId.length() < 2 || customerId.length() > 50) {
            throw new IllegalArgumentException("Customer ID must be between 2 and 50 characters");
        }
        if (serviceType == null || serviceType.isBlank()) {
            throw new IllegalArgumentException("Service type cannot be null or blank");
        }
        if (serviceType.length() < 3 || serviceType.length() > 100) {
            throw new IllegalArgumentException("Service type must be between 3 and 100 characters");
        }
    }

    // --- Static factory method for booking ---
    public static Appointment book(UUID slotRef, String branchId, String customerId, String serviceType) {
        return new Appointment(slotRef, branchId, customerId, serviceType);
    }

    // --- Business Methods (State Transitions) ---

    public void checkIn(LocalDateTime currentTime) {
        validateTimeInput(currentTime, "Current time");

        if (this.status != AppointmentStatus.BOOKED) {
            throw new IllegalStateException(
                    "Cannot check in. Appointment must be booked. Current status: " + this.status
            );
        }

        this.status = AppointmentStatus.CHECKED_IN;
        this.checkedInAt = currentTime;
        this.updatedAt = currentTime;
        increaseVersion();
    }

    public void startService(String consultantId, LocalDateTime currentTime) {
        if (consultantId == null || consultantId.isBlank()) {
            throw new IllegalArgumentException("Consultant ID cannot be null or blank");
        }
        if (consultantId.length() < 2 || consultantId.length() > 50) {
            throw new IllegalArgumentException("Consultant ID must be between 2 and 50 characters");
        }
        validateTimeInput(currentTime, "Current time");

        if (this.status != AppointmentStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Cannot start service. Appointment must be checked in. Current status: " + this.status
            );
        }

        this.status = AppointmentStatus.IN_PROGRESS;
        this.assignedConsultantId = consultantId;
        this.inProgressAt = currentTime;
        this.updatedAt = currentTime;
        increaseVersion();
    }

    public void complete(String consultantNotes, LocalDateTime currentTime) {
        validateTimeInput(currentTime, "Current time");

        if (consultantNotes == null || consultantNotes.isBlank()) {
            throw new IllegalArgumentException("Consultant notes cannot be null or blank");
        }
        if (consultantNotes.length() > 1000) {
            throw new IllegalArgumentException("Consultant notes cannot exceed 1000 characters");
        }

        if (this.status != AppointmentStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Cannot complete. Appointment must be in progress. Current status: " + this.status
            );
        }

        this.status = AppointmentStatus.COMPLETED;
        this.serviceNotes = consultantNotes;
        this.completedAt = currentTime;
        this.updatedAt = currentTime;
        increaseVersion();
    }

    public void cancelByCustomer(String reason, LocalDateTime currentTime) {
        validateTimeInput(currentTime, "Current time");

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or blank");
        }
        if (reason.length() > 500) {
            throw new IllegalArgumentException("Cancellation reason cannot exceed 500 characters");
        }

        if (!canBeCancelledByCustomer(currentTime)) {
            throw new IllegalStateException(
                    "Appointment cannot be canceled. Cancellation window has closed. " +
                            "Must cancel at least " + CANCELLATION_WINDOW_MINUTES + " minutes before appointment."
            );
        }

        if (this.status != AppointmentStatus.BOOKED && this.status != AppointmentStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Cannot cancel appointment with status " + this.status +
                            ". Only BOOKED or CHECKED_IN appointments can be cancelled."
            );
        }

        this.status = AppointmentStatus.CANCELLED;
        this.terminatedBy = this.customerUsername;
        this.terminationReason = AppointmentTerminationReason.CUSTOMER_CANCELLATION;
        this.terminationNotes = reason;
        this.terminatedAt = currentTime;
        this.updatedAt = currentTime;
        increaseVersion();
    }

    public void cancelByStaff(String staffId, String reason, LocalDateTime currentTime) {
        if (staffId == null || staffId.isBlank()) {
            throw new IllegalArgumentException("Staff ID cannot be null or blank");
        }
        if (staffId.length() < 2 || staffId.length() > 50) {
            throw new IllegalArgumentException("Staff ID must be between 2 and 50 characters");
        }
        validateTimeInput(currentTime, "Current time");

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or blank");
        }
        if (reason.length() > 500) {
            throw new IllegalArgumentException("Cancellation reason cannot exceed 500 characters");
        }

        if (this.status == AppointmentStatus.COMPLETED ||
                this.status == AppointmentStatus.CANCELLED ||
                this.status == AppointmentStatus.NO_SHOW) {
            throw new IllegalStateException(
                    "Cannot cancel a " + this.status + " appointment. " +
                            "Only BOOKED, CHECKED_IN, or IN_PROGRESS appointments can be cancelled by staff."
            );
        }

        this.status = AppointmentStatus.CANCELLED;
        this.terminatedBy = staffId;
        this.terminationReason = AppointmentTerminationReason.STAFF_CANCELLATION;
        this.terminationNotes = reason;
        this.terminatedAt = currentTime;
        this.updatedAt = currentTime;
        increaseVersion();
    }

    public void reschedule(UUID newSlotId, LocalDateTime currentTime) {
        if (newSlotId == null) {
            throw new IllegalArgumentException("New slot ID cannot be null");
        }
        validateTimeInput(currentTime, "Current time");

        if (!canBeRescheduled(currentTime)) {
            throw new IllegalStateException(
                    "Cannot reschedule after the cancellation window deadline. " +
                            "Appointment will be marked as no-show and slot will be forfeited."
            );
        }

        if (this.rescheduleCount >= MAX_RESCHEDULE_COUNT) {
            throw new IllegalStateException(
                    "Maximum reschedule limit (" + MAX_RESCHEDULE_COUNT + ") exceeded. " +
                            "Customer must book a new appointment."
            );
        }

        if (this.status != AppointmentStatus.BOOKED) {
            throw new IllegalStateException(
                    "Only BOOKED appointments can be rescheduled. Current status: " + this.status
            );
        }

        if (newSlotId.equals(this.slotId)) {
            throw new IllegalArgumentException(
                    "New slot ID must be different from current slot ID"
            );
        }

        this.previousSlotId = this.slotId;
        this.slotId = newSlotId;
        this.rescheduleCount++;
        this.updatedAt = currentTime;
        increaseVersion();
    }

    public void markAsNoShow(LocalDateTime currentTime) {
        validateTimeInput(currentTime, "Current time");

        if (this.status != AppointmentStatus.BOOKED && this.status != AppointmentStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Can only mark BOOKED or CHECKED_IN appointment as no-show. Current status: " + this.status
            );
        }

        this.status = AppointmentStatus.NO_SHOW;
        this.terminationReason = AppointmentTerminationReason.CUSTOMER_NO_SHOW;
        this.terminatedAt = currentTime;
        this.updatedAt = currentTime;
        increaseVersion();
    }

    // --- Query Methods ---

    public boolean canBeCancelledByCustomer(LocalDateTime currentTime) {
        if (currentTime == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        return true;
    }

    public boolean canBeRescheduled(LocalDateTime currentTime) {
        if (currentTime == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        return true;
    }

    public boolean isUpcoming(LocalDateTime currentTime) {
        if (currentTime == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        return (this.status == AppointmentStatus.BOOKED ||
                this.status == AppointmentStatus.CHECKED_IN) &&
                !isPastAppointmentTime(currentTime);
    }

    public boolean isPastAppointmentTime(LocalDateTime currentTime) {
        if (currentTime == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        return false;
    }

    public boolean isWithinGraceWindow(LocalDateTime currentTime, LocalDateTime slotStartTime) {
        validateTimeInput(currentTime, "Current time");
        validateTimeInput(slotStartTime, "Slot start time");

        Duration timeDifference = Duration.between(slotStartTime, currentTime);
        long minutes = timeDifference.toMinutes();
        return minutes >= 0 && minutes <= GRACE_WINDOW_MINUTES;
    }

    public boolean meetsMinimumBookingAdvance(LocalDateTime slotStartTime) {
        if (slotStartTime == null) {
            throw new IllegalArgumentException("Slot start time cannot be null");
        }

        Duration durationUntilSlot = Duration.between(this.createdAt, slotStartTime);
        return durationUntilSlot.toMinutes() >= MIN_BOOKING_ADVANCE_MINUTES;
    }

    // --- Concurrency Control (Optimistic Locking) ---

    private void increaseVersion() {
        this.version++;
    }

    public void validateVersion(int expectedVersion) {
        if (this.version != expectedVersion) {
            throw new IllegalStateException(
                    "Version mismatch. Expected: " + expectedVersion + ", Actual: " + this.version +
                            ". Another user may have modified this appointment."
            );
        }
    }

    // --- Helper Methods ---

    private static void validateTimeInput(LocalDateTime dateTime, String fieldName) {
        if (dateTime == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    private String generateBookingReference() {
        if (this.createdAt == null) {
            throw new IllegalStateException("Created timestamp cannot be null");
        }

        int year = this.createdAt.getYear();
        long sequence = getNextSequence();
        String reference = String.format("%s-%d-%0" + BOOKING_REF_SEQUENCE_LENGTH + "d",
                BOOKING_REF_PREFIX, year, sequence);

        if (reference.length() > 20) {
            throw new IllegalStateException("Generated booking reference exceeds maximum length");
        }

        return reference;
    }

    private long getNextSequence() {
        long sequence = (long) (Math.random() * 9999999);
        if (sequence < 0 || sequence > 9999999) {
            throw new IllegalStateException("Generated sequence is out of valid range");
        }
        return sequence;
    }

    // --- Getters ---

    public UUID getId() { return id; }
    public UUID getSlotId() { return slotId; }
    public String getBranchId() { return branchId; }
    public String getCustomerUsername() { return customerUsername; }
    public String getServiceType() { return serviceType; }
    public AppointmentStatus getStatus() { return status; }
    public String getBookingReference() { return bookingReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public LocalDateTime getInProgressAt() { return inProgressAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getTerminatedAt() { return terminatedAt; }
    public String getTerminatedBy() { return terminatedBy; }
    public AppointmentTerminationReason getTerminationReason() { return terminationReason; }
    public String getTerminationNotes() { return terminationNotes; }
    public String getAssignedConsultantId() { return assignedConsultantId; }
    public String getServiceNotes() { return serviceNotes; }
    public UUID getPreviousSlotId() { return previousSlotId; }
    public int getRescheduleCount() { return rescheduleCount; }
    public int getVersion() { return version; }

    // --- Equality and Hashing ---

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Appointment appointment)) return false;
        return Objects.equals(id, appointment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", slotId=" + slotId +
                ", branchId='" + branchId + '\'' +
                ", customerId='" + customerUsername + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", status=" + status +
                ", bookingReference='" + bookingReference + '\'' +
                ", assignedConsultantId='" + assignedConsultantId + '\'' +
                ", rescheduleCount=" + rescheduleCount +
                ", version=" + version +
                '}';
    }
}
