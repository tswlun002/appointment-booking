package capitec.branch.appointment.user.domain;

import java.util.Optional;

public interface ClientDomain {

    Optional<UserClientDetails> findByUsername(String IDNumber);
}
