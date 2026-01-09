package capitec.branch.appointment.appointment.infrastructure.dao;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Set;

@Repository
interface AppointmentRepository extends CrudRepository<AppointmentEntity, String> {

    @Query("""
        
            SELECT
            id,
            slot_id,
            branch_id,
            customer_username,
            service_type,
            status,
            booking_reference,
            version,
            created_at,
            updated_at,
            checked_in_at,
            in_progress_at,
            completed_at,
            terminated_at,
            terminated_by,
            termination_reason,
            termination_notes,
            assigned_consultant_id,
            service_notes,
            previous_slot_id,
            reschedule_count
        FROM
            appointment
        WHERE
            branch_id =:branchId
        """)
    Set<AppointmentEntity> getBranchAppointments(@Param("branchId") String branchId);
}
