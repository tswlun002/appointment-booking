package capitec.branch.appointment.user.app.port;
import java.util.Optional;
public interface CapitecClientPort {
    Optional<CapitecClientDetails> findByIdNumber(String idNumber);
}
