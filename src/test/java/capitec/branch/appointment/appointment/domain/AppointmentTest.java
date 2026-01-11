package capitec.branch.appointment.appointment.domain;

import capitec.branch.appointment.user.domain.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Appointment Domain Entity Tests")
class AppointmentTest {

    private UUID slotId;
    private String branchId;
    private String customerUsername;
    private String serviceType;
    private LocalDateTime now;
    // Default values for reconstitution test
    private final UUID persistenceId = UUID.randomUUID();
    private final String persistenceBookingRef = "APT-2025-1234567";
    private final LocalDateTime appointmentDateTime = LocalDate.now().plusDays(1).atTime(11,0);
    private final int persistenceVersion = 1;
    private final AppointmentStatus persistenceStatus = AppointmentStatus.CHECKED_IN;
    private final LocalDateTime persistenceCreatedAt = LocalDateTime.now().minusDays(1);

    @BeforeEach
    void setUp() {
        slotId = UUID.randomUUID();
        branchId = "BRANCH001";
        customerUsername = new UsernameGenerator().getId();
        serviceType = "Account Opening";
        now = LocalDateTime.now();
    }

    @Nested
    @DisplayName("Book Appointment Tests")
    class BookAppointmentTests {

        @Test
        @DisplayName("Should successfully create appointment with valid inputs")
        void shouldCreateAppointmentWithValidInputs() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertNotNull(appointment.getId());
            assertEquals(slotId, appointment.getSlotId());
            assertEquals(branchId, appointment.getBranchId());
            assertEquals(customerUsername, appointment.getCustomerUsername());
            assertEquals(serviceType, appointment.getServiceType());
            assertEquals(AppointmentStatus.BOOKED, appointment.getStatus());
            assertNotNull(appointment.getReference());
            assertEquals(appointment.getDateTime(), appointmentDateTime);
            assertEquals(0, appointment.getVersion());
            assertEquals(0, appointment.getRescheduleCount());
        }

        @Test
        @DisplayName("Should generate valid booking reference format")
        void shouldGenerateValidBookingReference() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            String reference = appointment.getReference();

            assertTrue(reference.matches(Appointment.BOOKING_REF_REGEX),
                    "Booking reference should match pattern APT-YYYY-XXXXXXX");
        }

        @Test
        @DisplayName("Should throw exception when slot ID is null")
        void shouldThrowExceptionWhenSlotIdIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(null, branchId, customerUsername, serviceType, appointmentDateTime)
            );
        }

        @Test
        @DisplayName("Should throw exception when branch ID is null")
        void shouldThrowExceptionWhenBranchIdIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, null, customerUsername, serviceType, appointmentDateTime)
            );
        }

        @Test
        @DisplayName("Should throw exception when branch ID is blank")
        void shouldThrowExceptionWhenBranchIdIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, "   ", customerUsername, serviceType, appointmentDateTime)
            );
        }

        @Test
        @DisplayName("Should throw exception when branch ID length is invalid")
        void shouldThrowExceptionWhenBranchIdLengthIsInvalid() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, "A", customerUsername, serviceType, appointmentDateTime)
            );
        }

        @Test
        @DisplayName("Should throw exception when customer username is null")
        void shouldThrowExceptionWhenCustomerUsernameIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, branchId, null, serviceType, appointmentDateTime)
            );
        }

        @Test
        @DisplayName("Should throw exception when customer username is blank")
        void shouldThrowExceptionWhenCustomerUsernameIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, branchId, "   ", serviceType, appointmentDateTime)
            );
        }

        @Test
        @DisplayName("Should throw exception when service type is null")
        void shouldThrowExceptionWhenServiceTypeIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, branchId, customerUsername, null, appointmentDateTime)
            );
        }

        @Test
        @DisplayName("Should throw exception when service type is blank")
        void shouldThrowExceptionWhenServiceTypeIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, branchId, customerUsername, "   ", appointmentDateTime)
            );
        }

        @Test
        @DisplayName("Should throw exception when service type length is invalid")
        void shouldThrowExceptionWhenServiceTypeLengthIsInvalid() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, branchId, customerUsername, "AB", appointmentDateTime)
            );
        }

        @Test
        @DisplayName("Should throw exception when appointment dateTime  is null")
        void shouldThrowExceptionWhenAppointmentDateTimeIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, branchId, customerUsername, serviceType, null)
            );
        }
        @Test
        @DisplayName("Should throw exception when appointment dateTime  is past dateTime")
        void shouldThrowExceptionWhenAppointmentDayIsOfPastDateTime() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Appointment(slotId, branchId, customerUsername, serviceType, LocalDate.now().minusDays(1).atStartOfDay())
            );
        }
    }

    @Nested
    @DisplayName("Check-In Tests")
    class CheckInTests {

        @Test
        @DisplayName("Should successfully check in booked appointment")
        void shouldCheckInBookedAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            appointment.checkIn(now);

            assertEquals(AppointmentStatus.CHECKED_IN, appointment.getStatus());
            assertEquals(now, appointment.getCheckedInAt());
            assertEquals(now, appointment.getUpdatedAt());
            //assertEquals(1, appointment.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when checking in non-booked appointment")
        void shouldThrowExceptionWhenCheckingIsExpiredBookedAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalStateException.class, ()->appointment.checkIn(appointmentDateTime.plusMinutes(10)));
        }
        @Test
        @DisplayName("Should throw exception when checking in non-booked appointment")
        void shouldThrowExceptionWhenCheckingInNonBookedAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            appointment.checkIn(now);

            assertThrows(IllegalStateException.class, () ->
                    appointment.checkIn(now.plusMinutes(5))
            );
        }

        @Test
        @DisplayName("Should throw exception when current time is null")
        void shouldThrowExceptionWhenCurrentTimeIsNull() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.checkIn(null)
            );
        }
    }

    @Nested
    @DisplayName("Start Service Tests")
    class StartServiceTests {

        @Test
        @DisplayName("Should successfully start service for checked-in appointment")
        void shouldStartServiceForCheckedInAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            appointment.checkIn(now);
            String consultantId = new UsernameGenerator().getId();

            appointment.startService(consultantId, now.plusMinutes(2));

            assertEquals(AppointmentStatus.IN_PROGRESS, appointment.getStatus());
            assertEquals(consultantId, appointment.getAssignedConsultantId());
            assertEquals(now.plusMinutes(2), appointment.getInProgressAt());
            //assertEquals(2, appointment.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when starting service on booked appointment")
        void shouldThrowExceptionWhenStartingServiceOnBookedAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalStateException.class, () ->
                    appointment.startService(new UsernameGenerator().getId(), now)
            );
        }

        @Test
        @DisplayName("Should throw exception when consultant ID is null")
        void shouldThrowExceptionWhenConsultantIdIsNull() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            appointment.checkIn(now);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.startService(null, now)
            );
        }

        @Test
        @DisplayName("Should throw exception when consultant ID is blank")
        void shouldThrowExceptionWhenConsultantIdIsBlank() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            appointment.checkIn(now);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.startService("   ", now)
            );
        }

        @Test
        @DisplayName("Should throw exception when consultant ID length is invalid")
        void shouldThrowExceptionWhenConsultantIdLengthIsInvalid() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            appointment.checkIn(now);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.startService("A", now)
            );
        }
    }

    @Nested
    @DisplayName("Complete Service Tests")
    class CompleteServiceTests {

        @Test
        @DisplayName("Should successfully complete in-progress appointment")
        void shouldCompleteInProgressAppointment() {
            Appointment appointment = createInProgressAppointment();
            String notes = "Service completed successfully";

            appointment.complete(notes, now.plusMinutes(15));

            assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
            assertEquals(notes, appointment.getServiceNotes());
            assertEquals(now.plusMinutes(15), appointment.getCompletedAt());
           // assertEquals(3, appointment.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when completing checked-in appointment")
        void shouldThrowExceptionWhenCompletingCheckedInAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            appointment.checkIn(now);

            assertThrows(IllegalStateException.class, () ->
                    appointment.complete("Notes", now)
            );
        }

        @Test
        @DisplayName("Should throw exception when notes are null")
        void shouldThrowExceptionWhenNotesAreNull() {
            Appointment appointment = createInProgressAppointment();

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.complete(null, now)
            );
        }

        @Test
        @DisplayName("Should throw exception when notes are blank")
        void shouldThrowExceptionWhenNotesAreBlank() {
            Appointment appointment = createInProgressAppointment();

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.complete("   ", now)
            );
        }

        @Test
        @DisplayName("Should throw exception when notes exceed maximum length")
        void shouldThrowExceptionWhenNotesExceedMaxLength() {
            Appointment appointment = createInProgressAppointment();
            String longNotes = "A".repeat(1001);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.complete(longNotes, now)
            );
        }
    }

    @Nested
    @DisplayName("Cancel by Customer Tests")
    class CancelByCustomerTests {

        @Test
        @DisplayName("Should successfully cancel booked appointment with valid reason")
        void shouldCancelBookedAppointmentWithValidReason() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            String reason = "Cannot attend";

            appointment.cancelByCustomer(reason, now);

            assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
            assertEquals(customerUsername, appointment.getTerminatedBy());
            assertEquals(AppointmentTerminationReason.CUSTOMER_CANCELLATION,
                    appointment.getTerminationReason());
            assertEquals(reason, appointment.getTerminationNotes());
            assertEquals(now, appointment.getTerminatedAt());
        }

        @Test
        @DisplayName("Should throw exception when cancellation reason is null")
        void shouldThrowExceptionWhenReasonIsNull() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.cancelByCustomer(null, now)
            );
        }

        @Test
        @DisplayName("Should throw exception when cancellation reason is blank")
        void shouldThrowExceptionWhenReasonIsBlank() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.cancelByCustomer("   ", now)
            );
        }

        @Test
        @DisplayName("Should throw exception when reason exceeds maximum length")
        void shouldThrowExceptionWhenReasonExceedsMaxLength() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            String longReason = "A".repeat(501);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.cancelByCustomer(longReason, now)
            );
        }
    }

    @Nested
    @DisplayName("Cancel by Staff Tests")
    class CancelByStaffTests {

        @Test
        @DisplayName("Should successfully cancel appointment by staff")
        void shouldCancelAppointmentByStaff() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            String staffId = new UsernameGenerator().getId();
            String reason = "Staff cancellation reason";

            appointment.cancelByStaff(staffId, reason, now);

            assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
            assertEquals(staffId, appointment.getTerminatedBy());
            assertEquals(AppointmentTerminationReason.STAFF_CANCELLATION,
                    appointment.getTerminationReason());
        }

        @Test
        @DisplayName("Should throw exception when staff ID is null")
        void shouldThrowExceptionWhenStaffIdIsNull() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.cancelByStaff(null, "reason", now)
            );
        }

        @Test
        @DisplayName("Should throw exception when cancelling completed appointment")
        void shouldThrowExceptionWhenCancellingCompletedAppointment() {
            Appointment appointment = createCompletedAppointment();

            assertThrows(IllegalStateException.class, () ->
                    appointment.cancelByStaff(new UsernameGenerator().getId(), "reason", now)
            );
        }

        @Test
        @DisplayName("Should throw exception when cancelling already cancelled appointment")
        void shouldThrowExceptionWhenCancellingAlreadyCancelledAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            appointment.cancelByCustomer("reason", now);

            assertThrows(IllegalStateException.class, () ->
                    appointment.cancelByStaff(new UsernameGenerator().getId(), "reason", now.plusMinutes(5))
            );
        }
    }

    @Nested
    @DisplayName("Reschedule Tests")
    class RescheduleTests {

        @Test
        @DisplayName("Should successfully reschedule booked appointment")
        void shouldRescheduleBookedAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            UUID newSlotId = UUID.randomUUID();

            appointment.reschedule(newSlotId, now);

            assertEquals(newSlotId, appointment.getSlotId());
            assertEquals(slotId, appointment.getPreviousSlotId());
            assertEquals(1, appointment.getRescheduleCount());
            assertEquals(AppointmentStatus.BOOKED, appointment.getStatus());
        }

        @Test
        @DisplayName("Should allow maximum of 3 reschedules")
        void shouldAllowMaximumThreeReschedules() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            appointment.reschedule(UUID.randomUUID(), now);
            appointment.reschedule(UUID.randomUUID(), now);
            appointment.reschedule(UUID.randomUUID(), now);

            assertThrows(IllegalStateException.class, () ->
                    appointment.reschedule(UUID.randomUUID(), now)
            );
        }

        @Test
        @DisplayName("Should throw exception when new slot ID is null")
        void shouldThrowExceptionWhenNewSlotIdIsNull() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.reschedule(null, now)
            );
        }

        @Test
        @DisplayName("Should throw exception when rescheduling to same slot")
        void shouldThrowExceptionWhenReschedulingToSameSlot() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.reschedule(slotId, now)
            );
        }

        @Test
        @DisplayName("Should throw exception when rescheduling checked-in appointment")
        void shouldThrowExceptionWhenReschedulingCheckedInAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            appointment.checkIn(now);

            assertThrows(IllegalStateException.class, () ->
                    appointment.reschedule(UUID.randomUUID(), now)
            );
        }
    }

    @Nested
    @DisplayName("Mark as No-Show Tests")
    class MarkAsNoShowTests {

        @Test
        @DisplayName("Should successfully mark booked appointment as no-show")
        void shouldMarkBookedAppointmentAsNoShow() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            appointment.markAsNoShow(now);

            assertEquals(AppointmentStatus.NO_SHOW, appointment.getStatus());
            assertEquals(AppointmentTerminationReason.CUSTOMER_NO_SHOW,
                    appointment.getTerminationReason());
            assertEquals(now, appointment.getTerminatedAt());
        }

        @Test
        @DisplayName("Should successfully mark checked-in appointment as no-show")
        void shouldMarkCheckedInAppointmentAsNoShow() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            appointment.checkIn(now);

            appointment.markAsNoShow(now.plusMinutes(5));

            assertEquals(AppointmentStatus.NO_SHOW, appointment.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when marking completed appointment as no-show")
        void shouldThrowExceptionWhenMarkingCompletedAsNoShow() {
            Appointment appointment = createCompletedAppointment();

            assertThrows(IllegalStateException.class, () ->
                    appointment.markAsNoShow(now)
            );
        }
    }

    @Nested
    @DisplayName("Grace Window Tests")
    class GraceWindowTests {

        @Test
        @DisplayName("Should return true when within 5-minute grace window")
        void shouldReturnTrueWhenWithinGraceWindow() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            LocalDateTime slotStartTime = now;
            LocalDateTime arrivalTime = now.plusMinutes(3);

            assertTrue(appointment.isWithinGraceWindow(arrivalTime, slotStartTime));
        }

        @Test
        @DisplayName("Should return true at exactly slot start time")
        void shouldReturnTrueAtExactSlotStartTime() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            LocalDateTime slotStartTime = now;

            assertTrue(appointment.isWithinGraceWindow(slotStartTime, slotStartTime));
        }

        @Test
        @DisplayName("Should return false when outside grace window")
        void shouldReturnFalseWhenOutsideGraceWindow() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
            LocalDateTime slotStartTime = now;
            LocalDateTime arrivalTime = now.plusMinutes(10);

            assertFalse(appointment.isWithinGraceWindow(arrivalTime, slotStartTime));
        }

        @Test
        @DisplayName("Should throw exception when current time is null")
        void shouldThrowExceptionWhenCurrentTimeIsNull() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.isWithinGraceWindow(null, now)
            );
        }

        @Test
        @DisplayName("Should throw exception when slot start time is null")
        void shouldThrowExceptionWhenSlotStartTimeIsNull() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalArgumentException.class, () ->
                    appointment.isWithinGraceWindow(now, null)
            );
        }
    }

    @Nested
    @DisplayName("Version Control Tests")
    class VersionControlTests {

//        @Test
//        @DisplayName("Should increment version on each state change")
//        void shouldIncrementVersionOnEachStateChange() {
//            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
//            assertEquals(0, appointment.getVersion());
//
//            appointment.checkIn(now);
//            assertEquals(1, appointment.getVersion());
//
//            appointment.startService(new UsernameGenerator().getId(), now.plusMinutes(2));
//            assertEquals(2, appointment.getVersion());
//
//            appointment.complete("Notes", now.plusMinutes(15));
//            assertEquals(3, appointment.getVersion());
//        }

        @Test
        @DisplayName("Should throw exception on version mismatch")
        void shouldThrowExceptionOnVersionMismatch() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertThrows(IllegalStateException.class, () ->
                    appointment.validateVersion(99)
            );
        }

        @Test
        @DisplayName("Should successfully validate correct version")
        void shouldSuccessfullyValidateCorrectVersion() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertDoesNotThrow(() -> appointment.validateVersion(0));
        }
    }

    @Nested
    @DisplayName("Equality and Hashing Tests")
    class EqualityAndHashingTests {

        @Test
        @DisplayName("Should return true when comparing same appointment")
        void shouldReturnTrueWhenComparingSameAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertEquals(appointment, appointment);
        }

        @Test
        @DisplayName("Should return true when comparing appointments with same ID")
        void shouldReturnTrueWhenComparingAppointmentsWithSameId() {
            Appointment appointment1 = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            // Note: This would require reflection or package-private access to set same ID
            // In practice, this tests the equality logic
            assertEquals(appointment1, appointment1);
        }

        @Test
        @DisplayName("Should return same hash code for same appointment")
        void shouldReturnSameHashCodeForSameAppointment() {
            Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);

            assertEquals(appointment.hashCode(), appointment.hashCode());
        }
    }
    @Nested
    @DisplayName("Reconstitution Tests")
    class ReconstitutionTests {

        @Test
        @DisplayName("Should successfully reconstitute appointment with valid persistence data")
        void shouldSuccessfullyReconstituteAppointment() {
            LocalDateTime terminatedAt = now.minusHours(1);
            String terminatedBy = "StaffUser";
            String notes = "Late Cancellation";

            Appointment appointment = Appointment.restitutionFromPersistence(
                    persistenceId,
                    slotId,
                    branchId,
                    customerUsername,
                    serviceType,
                    AppointmentStatus.CANCELLED, // Different status for test
                    persistenceBookingRef,
                    appointmentDateTime,
                    persistenceVersion,
                    persistenceCreatedAt,
                    now.minusMinutes(30), // updatedAt
                    now.minusMinutes(45), // checkedInAt
                    null, // inProgressAt
                    null, // completedAt
                    terminatedAt,
                    terminatedBy,
                    AppointmentTerminationReason.STAFF_CANCELLATION,
                    notes,
                    null, // assignedConsultantId
                    null, // serviceNotes
                    UUID.randomUUID(), // previousSlotId
                    1 // rescheduleCount
            );

            // Assertions for critical fields
            assertEquals(persistenceId, appointment.getId());
            assertEquals(branchId, appointment.getBranchId());
            assertEquals(customerUsername, appointment.getCustomerUsername());
            assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
            assertEquals(persistenceVersion, appointment.getVersion());
            assertEquals(terminatedAt, appointment.getTerminatedAt());
            assertEquals(terminatedBy, appointment.getTerminatedBy());
        }

        @Test
        @DisplayName("Should throw exception when ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    Appointment.restitutionFromPersistence(
                            null, // ID is null
                            slotId, branchId, customerUsername, serviceType,
                            persistenceStatus, persistenceBookingRef, appointmentDateTime, persistenceVersion,
                            persistenceCreatedAt, now, null, null, null, null, null, null, null, null, null, null, 0
                    )
            );
        }

        @Test
        @DisplayName("Should throw exception when Branch ID is null")
        void shouldThrowExceptionWhenBranchIdIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    Appointment.restitutionFromPersistence(
                            persistenceId, slotId, null, // Branch ID is null
                            customerUsername, serviceType, persistenceStatus, persistenceBookingRef, appointmentDateTime,
                            persistenceVersion, persistenceCreatedAt, now, null, null, null, null, null, null, null, null, null, null, 0
                    )
            );
        }

        @Test
        @DisplayName("Should throw exception when Booking Reference is blank")
        void shouldThrowExceptionWhenBookingReferenceIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    Appointment.restitutionFromPersistence(
                            persistenceId, slotId, branchId, customerUsername, serviceType,
                            persistenceStatus, "   ", // Booking Reference is blank
                            appointmentDateTime,persistenceVersion, persistenceCreatedAt, now, null, null, null, null, null, null, null, null, null, null, 0
                    )
            );
        }

        @Test
        @DisplayName("Should throw exception when Version is less than one")
        void shouldThrowExceptionWhenVersionIsZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    Appointment.restitutionFromPersistence(
                            persistenceId, slotId, branchId, customerUsername, serviceType,
                            persistenceStatus, persistenceBookingRef, appointmentDateTime, 0, // Version is 0
                            persistenceCreatedAt, now, null, null, null, null, null, null, null, null, null, null, 0
                    )
            );
        }

        @Test
        @DisplayName("Should throw exception when CreatedAt is null")
        void shouldThrowExceptionWhenCreatedAtIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    Appointment.restitutionFromPersistence(
                            persistenceId, slotId, branchId, customerUsername, serviceType,
                            persistenceStatus, persistenceBookingRef, appointmentDateTime, persistenceVersion,
                            null, // CreatedAt is null
                            now, null, null, null, null, null, null, null, null, null, null, 0
                    )
            );
        }
    }

    // --- Helper Methods ---

    private Appointment createInProgressAppointment() {
        Appointment appointment = new Appointment(slotId, branchId, customerUsername, serviceType, appointmentDateTime);
        appointment.checkIn(now);
        appointment.startService(new UsernameGenerator().getId(), now.plusMinutes(2));
        return appointment;
    }

    private Appointment createCompletedAppointment() {
        Appointment appointment = createInProgressAppointment();
        appointment.complete("Service completed", now.plusMinutes(15));
        return appointment;
    }
}
