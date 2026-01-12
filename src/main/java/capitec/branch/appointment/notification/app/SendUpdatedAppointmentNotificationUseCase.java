package capitec.branch.appointment.notification.app;

import capitec.branch.appointment.appointment.app.CustomerRescheduledAppointmentEvent;
import capitec.branch.appointment.notification.app.port.BranchLookup;
import capitec.branch.appointment.notification.app.port.CustomerLookup;
import capitec.branch.appointment.notification.domain.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendUpdatedAppointmentNotificationUseCase {

    private final BranchLookup branchLookup;
    private final CustomerLookup customerLookup;
    private final NotificationService notificationService;

    @EventListener
    public void onAppointmentUpdates(CustomerRescheduledAppointmentEvent event) {

        var branch = branchLookup.findById(event.branchId());
        var user = customerLookup.findByUsername(event.customerUsername());


        notificationService.se(branch,user, event);
    }

}
