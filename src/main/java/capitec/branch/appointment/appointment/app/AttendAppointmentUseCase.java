package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AttendingAppointmentStateTransitionAction;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@UseCase
@Slf4j
@RequiredArgsConstructor
@Validated
public class AttendAppointmentUseCase {

    private final AppointmentService appointmentService;

    public  void execute(AttendingAppointmentStateTransitionAction action){

        switch (action){

            case AttendingAppointmentStateTransitionAction.CheckIn(String branchId, LocalDate day, String customerUsername)->{

               var appointment = appointmentService.getUserActiveAppointment(branchId,day,customerUsername)
                       .orElseThrow(()->{
                           log.error("Appointment is not found. User:{} at branch: {} on date: {}",customerUsername,branchId,day);
                           return new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment is not found.");

                       });
               action.execute(appointment, LocalDateTime.now());
            }

            case AttendingAppointmentStateTransitionAction.StartService(UUID appointmentId, String staffUsername) -> {

                log.info("Staff {}  is starting appointment {}",staffUsername,appointmentId);
                var appointment =findById(appointmentId);
                action.execute(appointment, LocalDateTime.now());

            }
            case  AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(UUID appointmentId, _) ->{

                var appointment =  findById(appointmentId);
                action.execute(appointment, LocalDateTime.now());
            }
            case AttendingAppointmentStateTransitionAction.CancelByStaff(String staffUsername, _, UUID appointmentId)->{

                log.info("Cancel user appointment:{} by staff:{}",appointmentId,staffUsername);

                var appointment = findById(appointmentId);
                action.execute(appointment, LocalDateTime.now());
            }

        }

    }
    private Appointment findById(UUID appointmentId){
      return   appointmentService.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("Appointment is not found. Appointment id:{}", appointmentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment is not found.");
                });
    }
}
