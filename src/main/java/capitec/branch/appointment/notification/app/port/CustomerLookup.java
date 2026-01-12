package capitec.branch.appointment.notification.app.port;

public interface CustomerLookup {
    CustomerDetails findByUsername(String username);
}
