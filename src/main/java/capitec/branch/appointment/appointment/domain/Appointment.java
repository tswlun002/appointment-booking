package capitec.branch.appointment.appointment.domain;

import capitec.branch.appointment.sharekernel.username.UsernameGenerator;
import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.ValidatorMessages;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Appointment {

    // --- Validation Constants ---
    private static   final Logger LOGGER = LoggerFactory.getLogger(Appointment.class);
    private static final int MIN_BOOKING_ADVANCE_MINUTES = 60;
    private static final int CANCELLATION_WINDOW_MINUTES = 120;
    private static final int GRACE_WINDOW_MINUTES = 5;
    private static final int MAX_RESCHEDULE_COUNT = 3;
    private static final String BOOKING_REF_PREFIX = "APT";
    private static final int BOOKING_REF_YEAR_LENGTH = 4;
    private static final int BOOKING_REF_SEQUENCE_LENGTH = 7;
    protected static final String BOOKING_REF_REGEX = "^APT-\\d{"+BOOKING_REF_YEAR_LENGTH+"}-\\d{"+BOOKING_REF_SEQUENCE_LENGTH+"}$";
    private static final int BOOKING_MAX_REFERENCE_LENGTH = 20;

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

    @NotBlank(message = "Appointment reference cannot be blank")
    @Pattern(regexp = Appointment.BOOKING_REF_REGEX, message = "Appointment reference must match pattern: APT-YYYY-XXXXXXX")
    private final String reference;
    @NotNull(message = "Appointment dateTime cannot be null")
    private  LocalDateTime dateTime;
    @Min(value = 0, message = "Version cannot be negative")
    @Max(value = Integer.MAX_VALUE, message = "Version exceeds maximum allowed value")
    private int version;

    // --- Timestamps ---
    @NotNull(message = "Created createdAt cannot be null")
    @PastOrPresent(message = "Created createdAt cannot be in the future")
    private final LocalDateTime createdAt;

    @NotNull(message = "Updated createdAt cannot be null")
    @PastOrPresent(message = "Updated createdAt cannot be in the future")
    private LocalDateTime updatedAt;

    @PastOrPresent(message = "Checked-in createdAt cannot be in the future")
    private LocalDateTime checkedInAt;

    @PastOrPresent(message = "In-progress createdAt cannot be in the future")
    private LocalDateTime inProgressAt;

    @PastOrPresent(message = "Completed createdAt cannot be in the future")
    private LocalDateTime completedAt;

    @PastOrPresent(message = "Terminated createdAt cannot be in the future")
    private LocalDateTime terminatedAt;

    // --- Audit Trail ---
    @Size(min = 2, max = 50, message = "Terminated by ID must be between 2 and 50 characters")
    private String terminatedBy;

    private AppointmentTerminationReason terminationReason;

    @Size(max = 500, message = "Termination notes cannot exceed 500 characters")
    private String terminationNotes;

    // --- Service Execution ---
    private String assignedConsultantId;

    @Size(max = 1000, message = "Service notes cannot exceed 1000 characters")
    private String serviceNotes;

    // --- Rescheduling Support ---
    private UUID previousSlotId;

    @Min(value = 0, message = "Reschedule count cannot be negative")
    @Max(value = MAX_RESCHEDULE_COUNT, message = "Reschedule count cannot exceed " + MAX_RESCHEDULE_COUNT)
    private int rescheduleCount;


    public Appointment(UUID slotId, String branchId, String customerUsername, String serviceType,LocalDateTime dateTime) {
        validateConstructorInputs(slotId, branchId, customerUsername, serviceType, dateTime);

        this.id = UUID.randomUUID();
        this.slotId = slotId;
        this.branchId = branchId;
        this.customerUsername = customerUsername;
        this.serviceType = serviceType;
        this.dateTime = dateTime;
        this.status = AppointmentStatus.BOOKED;
        this.createdAt = LocalDateTime.now();
        this.reference = generateAppointmentReference();
        this.updatedAt = this.createdAt;
        this.version = 0;
        this.rescheduleCount = 0;
    }
    private Appointment(
            UUID id, UUID slotId, String branchId, String customerUsername, String serviceType,
            AppointmentStatus status, String reference, LocalDateTime dateTime, int version,
            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime checkedInAt,
            LocalDateTime inProgressAt, LocalDateTime completedAt, LocalDateTime terminatedAt,
            String terminatedBy, AppointmentTerminationReason terminationReason, String terminationNotes,
            String assignedConsultantId, String serviceNotes, UUID previousSlotId, int rescheduleCount) {

        this.id = id;
        this.slotId = slotId;
        this.branchId = branchId;
        this.customerUsername = customerUsername;
        this.serviceType = serviceType;
        this.status = status;
        this.reference = reference;
        this.dateTime = dateTime;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.checkedInAt = checkedInAt;
        this.inProgressAt = inProgressAt;
        this.completedAt = completedAt;
        this.terminatedAt = terminatedAt;
        this.terminatedBy = terminatedBy;
        this.terminationReason = terminationReason;
        this.terminationNotes = terminationNotes;
        this.assignedConsultantId = assignedConsultantId;
        this.serviceNotes = serviceNotes;
        this.previousSlotId = previousSlotId;
        this.rescheduleCount = rescheduleCount;
    }


    // --- Input Validation ---
    private static void validateConstructorInputs(UUID slotId, String branchId, String customerUsername, String serviceType,LocalDateTime dateTime) {

        Assert.notNull(slotId, "Slot ID cannot be null");
        // Branch ID checks
        Assert.hasText(branchId, "Branch ID cannot be null or blank");
        Assert.isTrue(branchId.length() >= 2 && branchId.length() <= 50,
                "Branch ID must be between 2 and 50 characters");

        // Customer ID checks
        Assert.isTrue(UsernameGenerator.isValid(customerUsername), ValidatorMessages.USERNAME_MESSAGE);

        // Service Type checks
        Assert.hasText(serviceType, "Service type cannot be null or blank");
        Assert.isTrue(serviceType.length() >= 3 && serviceType.length() <= 100,
                "Service type must be between 3 and 100 characters");
        // Appointment dateOfSlots
        Assert.isTrue(dateTime !=null, "Appointment dateTime cannot be null");
        Assert.isTrue(dateTime.isAfter(LocalDateTime.now()), "Appointment dateTime cannot be a past dateOfSlots");
    }

    // --- Business Methods (State Transitions) ---

    public void checkIn(LocalDateTime currentTime) {
        if(currentTime == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        LocalDateTime deadline = dateTime.plusMinutes(GRACE_WINDOW_MINUTES);
        if(currentTime.isAfter(deadline)) {

            LOGGER.error("Check-in failed. Appointment check deadline was {}", deadline);
            throw new IllegalStateException("Check-in failed. Appointment check deadline was "+deadline);
        }
        if (this.status != AppointmentStatus.BOOKED) {

            LOGGER.error("Cannot check in. Appointment must be booked. Current status: {}" , this.status);
            throw new IllegalStateException("Cannot check in. Appointment must be booked.");
        }

        this.status = AppointmentStatus.CHECKED_IN;
        this.checkedInAt = currentTime;
        this.updatedAt = currentTime;
        //increaseVersion();  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking
    }

    public void startService(String consultantId, LocalDateTime currentTime) {

        if (!UsernameGenerator.isValid(consultantId)) {

            LOGGER.error("Consultant ID is not valid. {}", ValidatorMessages.USERNAME_MESSAGE);
            throw new IllegalArgumentException("Consultant ID is not valid");
        }
        validateTimeInput(currentTime, "Current time");

        if (this.status != AppointmentStatus.CHECKED_IN) {

            LOGGER.error("Cannot start service. Appointment must be checked in. Current status:{}",this.status);
            throw new IllegalStateException("Cannot start service. Appointment must be checked in.");
        }

        this.status = AppointmentStatus.IN_PROGRESS;
        this.assignedConsultantId = consultantId;
        this.inProgressAt = currentTime;
        this.updatedAt = currentTime;
        //increaseVersion();  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking
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
            LOGGER.error("Cannot complete. Appointment must be in progress. Current status: {}" , this.status);
            throw new IllegalStateException("Cannot complete. Appointment must be in progress.");
        }

        this.status = AppointmentStatus.COMPLETED;
        this.serviceNotes = consultantNotes;
        this.completedAt = currentTime;
        this.updatedAt = currentTime;
        //increaseVersion();  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking
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

            LOGGER.error("Appointment cannot be canceled. Cancellation window has closed. " + "Must cancel at least " + CANCELLATION_WINDOW_MINUTES + " minutes before appointment.");
            throw new IllegalStateException("Appointment cannot be canceled. Cancellation window has closed.");
        }

        if (this.status != AppointmentStatus.BOOKED && this.status != AppointmentStatus.CHECKED_IN) {
            LOGGER.error("Cannot cancel appointment with status {}. Only BOOKED or CHECKED_IN appointments can be cancelled.", this.status);
            throw new IllegalStateException("Only BOOKED or CHECKED_IN appointments can be cancelled.");
        }

        this.status = AppointmentStatus.CANCELLED;
        this.terminatedBy = this.customerUsername;
        this.terminationReason = AppointmentTerminationReason.CUSTOMER_CANCELLATION;
        this.terminationNotes = reason;
        this.terminatedAt = currentTime;
        this.updatedAt = currentTime;
        //increaseVersion();  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking

    }

    //May need to cancel due to emergencies, system issues, customer requests at any time
    public void cancelByStaff(String staffId, String reason, LocalDateTime currentTime) {
        if (staffId == null || staffId.isBlank()) {
            throw new IllegalArgumentException("Staff ID cannot be null or blank");
        }
        if (staffId.length() < 2 || staffId.length() > 50) {
            throw new IllegalArgumentException("Staff ID must be between 2 and 50 characters");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or blank");
        }
        if (reason.length() > 500) {
            throw new IllegalArgumentException("Cancellation reason cannot exceed 500 characters");
        }

        if (this.status == AppointmentStatus.COMPLETED ||
                this.status == AppointmentStatus.CANCELLED ||
                this.status == AppointmentStatus.NO_SHOW) {

            LOGGER.error("Cannot cancel a {} appointment. Only BOOKED, CHECKED_IN, or IN_PROGRESS appointments can be cancelled by staff.", this.status);
            throw new IllegalStateException("Only BOOKED, CHECKED_IN, or IN_PROGRESS appointments can be cancelled by staff.");
        }

        this.status = AppointmentStatus.CANCELLED;
        this.terminatedBy = staffId;
        this.terminationReason = AppointmentTerminationReason.STAFF_CANCELLATION;
        this.terminationNotes = reason;
        this.terminatedAt = currentTime;
        this.updatedAt = currentTime;
        //increaseVersion();  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking

    }

    public void reschedule(UUID newSlotId,LocalDateTime newDateTime, LocalDateTime currentTime) {

        if (newSlotId == null) {
            throw new IllegalArgumentException("New slot ID cannot be null");
        }
        if (newDateTime == null) {
            throw new IllegalArgumentException("New appointment dateTime cannot be null");
        }
        if(currentTime == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }

        if(newDateTime.isBefore(currentTime)) {
            throw new IllegalArgumentException("New appointment dateTime cannot be in the past");
        }

        if (!canBeRescheduled(currentTime)) {

            LocalDateTime rescheduleDeadline  = dateTime.plusMinutes(MIN_BOOKING_ADVANCE_MINUTES);
            LOGGER.error("Cannot reschedule appointment after deadline {} which is {} minutes before appointment.",rescheduleDeadline,MIN_BOOKING_ADVANCE_MINUTES);

            throw new IllegalStateException("Cannot reschedule after the reschedule window deadline.");
        }

        if (this.rescheduleCount >= MAX_RESCHEDULE_COUNT) {
            LOGGER.error("Maximum reschedule limit (" + MAX_RESCHEDULE_COUNT + ") exceeded. " + "Customer must book a new appointment.");
            throw new IllegalStateException("Maximum reschedule limit (" + MAX_RESCHEDULE_COUNT + ") exceeded. " + "Customer must book a new appointment.");
        }

        if (this.status != AppointmentStatus.BOOKED) {
            LOGGER.error("Only BOOKED appointments can be rescheduled. Current status: {}" , this.status);
            throw new IllegalStateException("Only BOOKED appointments can be rescheduled.");
        }

        if (newSlotId.equals(this.slotId)) {
            LOGGER.error("New slot ID must be different from current slot ID. new slotId: {} , current slot:{}", newSlotId, this.slotId);
            throw new IllegalArgumentException("New slot ID must be different from current slot ID");
        }


        this.previousSlotId = this.slotId;
        this.slotId = newSlotId;
        this.dateTime = newDateTime;
        this.rescheduleCount++;
        this.updatedAt = currentTime;
        //increaseVersion();  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking
    }

    public void markAsNoShow(LocalDateTime currentTime) {
        if (currentTime == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        if (currentTime.isBefore(dateTime.plusMinutes(GRACE_WINDOW_MINUTES))) {
            throw new IllegalArgumentException("Cannot mark appointment as no show before start time plus grace window.");
        }

        if (this.status != AppointmentStatus.BOOKED && this.status != AppointmentStatus.CHECKED_IN) {
            LOGGER.error("Can only mark BOOKED or CHECKED_IN appointment as no-show. Current status: {}" , this.status);
            throw new IllegalStateException("Can only mark BOOKED or CHECKED_IN appointment as no-show.");
        }

        this.status = AppointmentStatus.NO_SHOW;
        this.terminationReason = AppointmentTerminationReason.CUSTOMER_NO_SHOW;
        this.terminatedAt = currentTime;
        this.updatedAt = currentTime;
        this.terminatedBy= "SYSTEM_SCHEDULER";
        //increaseVersion();  this managed by infrastructure, consider uncommenting if infrastructure does manage optimistic locking
    }

    // --- Query Methods ---

    public  boolean isRescheduled() {
        return getPreviousSlotId() !=null ;
    }

    public boolean canBeCancelledByCustomer(LocalDateTime currentTime) {
        if (currentTime == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        LocalDateTime deadline = dateTime.plusMinutes(MIN_BOOKING_ADVANCE_MINUTES);
        return currentTime.isBefore(deadline);
    }

    public boolean canBeRescheduled(LocalDateTime currentTime) {
       return  canBeCancelledByCustomer(currentTime);
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

    public boolean meetsMinimumAppointmentAdvance(LocalDateTime slotStartTime) {
        if (slotStartTime == null) {
            throw new IllegalArgumentException("Slot start time cannot be null");
        }

        Duration durationUntilSlot = Duration.between(this.dateTime, slotStartTime);
        return durationUntilSlot.toMinutes() >= MIN_BOOKING_ADVANCE_MINUTES;
    }

    /**
     * Concurrency Control (Optimistic Locking)
     */
    private void increaseVersion() {
        this.version++;
    }

    public void validateVersion(int expectedVersion) {
        if (this.version != expectedVersion) {
            LOGGER.error("Version mismatch. Expected: {}, Actual: {}. Another user may have modified this appointment.", expectedVersion, this.version);
            throw new IllegalStateException("Version mismatch. Another user may have modified this appointment.");
        }
    }

    // --- Helper Methods ---

    private  void validateTimeInput(LocalDateTime currentDateTime, String fieldName) {
        if (currentDateTime == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (currentDateTime.isAfter(dateTime)) {
            throw new IllegalArgumentException("Cannot modify appointment after the appointment dateTime.");
        }

    }

    private String generateAppointmentReference() {
        if (this.dateTime == null) {
            throw new IllegalStateException("Appointment dateOfSlots cannot be null");
        }

        int year = this.dateTime.getYear();
        long sequence = getNextSequence();
        String reference = String.format("%s-%d-%0" + BOOKING_REF_SEQUENCE_LENGTH + "d", BOOKING_REF_PREFIX, year, sequence);

        if (reference.length() > BOOKING_MAX_REFERENCE_LENGTH) {
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

    public static Appointment restitutionFromPersistence(
            UUID id,
            UUID slotId,
            String branchId,
            String customerUsername,
            String serviceType,
            AppointmentStatus status,
            String bookingReference,
            LocalDateTime dateTime,
            int version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime checkedInAt,
            LocalDateTime inProgressAt,
            LocalDateTime completedAt,
            LocalDateTime terminatedAt,
            String terminatedBy,
            AppointmentTerminationReason terminationReason,
            String terminationNotes,
            String assignedConsultantId,
            String serviceNotes,
            UUID previousSlotId,
            int rescheduleCount) {


        // --- END VALIDATION ---
        validateMustHaveInRestitution(id,slotId,branchId,customerUsername,serviceType,status,bookingReference,dateTime,version,createdAt);

        // Calls the private constructor with all validated state
        return new Appointment(
                id, slotId, branchId, customerUsername, serviceType, status,
                bookingReference, dateTime, version, createdAt, updatedAt, checkedInAt,
                inProgressAt, completedAt, terminatedAt, terminatedBy,
                terminationReason, terminationNotes, assignedConsultantId,
                serviceNotes, previousSlotId, rescheduleCount);
    }

    private  static void validateMustHaveInRestitution(            UUID id,
                                                            UUID slotId,
                                                            String branchId,
                                                            String customerUsername,
                                                            String serviceType,
                                                            AppointmentStatus status,
                                                            String reference,
                                                            LocalDateTime dateTime,
                                                            int version,
                                                            LocalDateTime createdAt){
        // --- 🎯 RESTITUTION VALIDATION ---
        if (id == null) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: ID must not be null.");
        }
        if (branchId == null || branchId.isBlank()) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: Branch ID must not be null or blank.");
        }
        if (customerUsername == null || customerUsername.isBlank()) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: Customer Username must not be null or blank.");
        }
        if(slotId == null) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: Slot ID must not be null.");
        }
        if(reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: Appointment Reference must not be null.");
        }
        if(dateTime == null) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: Appointment dateTime must not be null.");
        }
        // Assuming version must be >= 0 for a loaded record
        if (version < 1) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: Version must be greater than or equal to one.");
        }
        if(createdAt == null) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: CreatedAt must not be null.");
        }
        if(status == null) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: Status must not be null.");
        }
        if(serviceType == null) {
            throw new IllegalArgumentException("Cannot reconstitute Appointment: Service Type must not be null.");
        }


    }

    // --- Getters ---

    public UUID getId() { return id; }
    public UUID getSlotId() { return slotId; }
    public String getBranchId() { return branchId; }
    public String getCustomerUsername() { return customerUsername; }
    public String getServiceType() { return serviceType; }
    public AppointmentStatus getStatus() { return status; }
    public String getReference() { return reference; }
    public LocalDateTime getDateTime() { return dateTime; }
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
                ", dateTime='" + dateTime + '\'' +
                ", reference='" + reference + '\'' +
                ", assignedConsultantId='" + assignedConsultantId + '\'' +
                ", rescheduleCount=" + rescheduleCount +
                ", version=" + version +
                '}';
    }
}
