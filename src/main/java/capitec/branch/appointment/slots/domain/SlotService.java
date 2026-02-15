package capitec.branch.appointment.slots.domain;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Domain service for slot business operations.
 * Contains only business rules - no query concerns.
 */
public interface SlotService {

    @Transactional
    void save(List<Slot> slot);
}
