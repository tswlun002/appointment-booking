package capitec.branch.appointment.appointment.domain;

import capitec.branch.appointment.utils.Username;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public sealed interface AttendingAppointmentStateTransitionAction permits AttendingAppointmentStateTransitionAction.CheckIn,
        AttendingAppointmentStateTransitionAction.StartService, AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment,
   AttendingAppointmentStateTransitionAction.CancelByStaff{

   void execute(Appointment appointment, LocalDateTime currentTime);

    record CheckIn(String branchId, LocalDate day, String customerUsername) implements AttendingAppointmentStateTransitionAction {

        @Override
        public void execute(Appointment appointment, LocalDateTime currentTime) {
            appointment.checkIn(currentTime);
        }
    }

     record StartService(UUID appointmentId, @Username String consultantUsername) implements AttendingAppointmentStateTransitionAction {
         @Override
         public void execute(Appointment appointment, LocalDateTime currentTime) {
             appointment.startService(consultantUsername,currentTime);
         }
     }

    record CompleteAttendingAppointment(UUID appointmentId, String consultantNotes) implements AttendingAppointmentStateTransitionAction {
        @Override
        public void execute(Appointment appointment, LocalDateTime currentTime) {
            appointment.complete(consultantNotes,currentTime);
        }
    }

    record CancelByStaff(String staffUsername,String reason,UUID appointmentId) implements AttendingAppointmentStateTransitionAction {
        @Override
        public void execute(Appointment appointment, LocalDateTime currentTime) {
            appointment.cancelByStaff(staffUsername,reason,currentTime);
        }
    }
}
