package capitec.branch.appointment.appointment.domain;

public enum AppointmentStatus {
    BOOKED,           // Customer has booked the appointment
    CHECKED_IN,       // Customer arrived and checked in at branch
    IN_PROGRESS,      // Consultant assigned and serving the customer
    COMPLETED,        // Service completed
    CANCELLED,        // Appointment cancelled by customer or staff
    NO_SHOW,          // Customer did not arrive
    RESCHEDULED       // Customer rescheduled to another slot
}