package capitec.branch.appointment.appointment.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public sealed interface CustomerUpdateAppointmentAction permits CustomerUpdateAppointmentAction.Cancel,
        CustomerUpdateAppointmentAction.Reschedule{
    void execute(Appointment appointment, LocalDateTime currentTime);
    UUID getId();
    String getEventName();
    record Cancel(UUID appointmentId,String reason) implements CustomerUpdateAppointmentAction {

        @Override
        public void execute(Appointment appointment, LocalDateTime currentTime) {
            appointment.cancelByCustomer(reason, currentTime);
        }

        @Override
        public UUID getId() {
            return appointmentId;
        }

        @Override
        public String getEventName() {
            return "canceled";
        }
    }
    record Reschedule(UUID appointmentId,UUID newSlotId,LocalDateTime newDateTime) implements CustomerUpdateAppointmentAction {

        @Override
        public void execute(Appointment appointment, LocalDateTime currentTime) {
            appointment.reschedule(newSlotId,newDateTime,currentTime);
        }

        @Override
        public UUID getId() {
            return appointmentId;
        }

        @Override
        public String getEventName() {
            return "rescheduled";
        }
    }
}
