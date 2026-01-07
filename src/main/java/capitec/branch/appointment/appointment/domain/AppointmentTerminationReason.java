package capitec.branch.appointment.appointment.domain;

public enum AppointmentTerminationReason {
    CUSTOMER_CANCELLATION,    // Customer cancelled within allowed window
    CUSTOMER_NO_SHOW,         // Customer did not arrive
    STAFF_CANCELLATION,       // Staff/Manager cancelled
    CUSTOMER_RESCHEDULED      // Customer rescheduled to another slot
}