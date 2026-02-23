package capitec.branch.appointment.staff.app;


import java.util.Optional;

public interface UserPortService {

    Optional<String> execute(String username);
}
