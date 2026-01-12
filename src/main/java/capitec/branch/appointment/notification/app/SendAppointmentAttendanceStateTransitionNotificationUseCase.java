package capitec.branch.appointment.notification.app;

import capitec.branch.appointment.appointment.app.AppointmentStateChangedEvent;
import capitec.branch.appointment.notification.app.port.CustomerLookup;
import capitec.branch.appointment.notification.domain.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendAppointmentAttendanceStateTransitionNotificationUseCase {

    private final CustomerLookup customerLookup;
    private final NotificationService notificationService;

    @EventListener
    public void onAppointmentBooked(AppointmentStateChangedEvent  event) {

        var user = customerLookup.findByUsername(event.customerUsername());

        notificationService.sendAttendanceStateTransitionEmail(user, event);
    }

}
