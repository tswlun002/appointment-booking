package capitec.branch.appointment.notification.infrastructure.adapter;

import capitec.branch.appointment.notification.app.port.CustomerDetails;
import capitec.branch.appointment.notification.app.port.CustomerLookup;
import capitec.branch.appointment.user.app.GetUserQuery;
import capitec.branch.appointment.user.app.UsernameCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerLookupAdapter implements CustomerLookup {

    private final GetUserQuery getUserQuery;

    @Override
    public CustomerDetails findByUsername(String username) {
        var user = getUserQuery.execute(new UsernameCommand(username));
        return new CustomerDetails(username,user.getFirstname()+" "+user.getLastname(),user.getEmail());
    }
}
