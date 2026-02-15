package capitec.branch.appointment.appointment.infrastructure.dao;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
interface AppointmentRepository extends CrudRepository<AppointmentEntity, UUID> {

    @Query("""
        
            SELECT
            id,
            slot_id,
            branch_id,
            customer_username,
            service_type,
            status,
            reference,
            date_time,
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
            reschedule_count,
            COUNT(*) OVER () AS total_appointments_count
        FROM appointment
        WHERE branch_id =:branchId 
        ORDER BY date_time DESC
        OFFSET :pageNumber LIMIT :pageSize
        """)
    Set<AppointmentEntity> getBranchAppointments(@Param("branchId") String branchId, @Param("pageNumber") int pageNumber ,@Param("pageSize") int pageSize);
@Query("""
                SELECT
            id,
            slot_id,
            branch_id,
            customer_username,
            service_type,
            status,
            reference,
            date_time,
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
        WHERE  branch_id =:branchId AND customer_username=:customerUsername AND status IN('BOOKED', 'CHECKED_IN','IN_PROGRESS') AND day=:day 
    """)
    Optional<AppointmentEntity> getUserActiveAppointment(@Param("branchId")String branchId, @Param("day")LocalDate day,
                                                         @Param("customerUsername") String customerUsername);


    @Query("""
            SELECT 
            id,
            slot_id,
            branch_id,
            customer_username,
            service_type,
            status,
            reference,
            date_time,
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
            FROM appointment
            WHERE DATE(date_time) = :appointmentDate
              AND status IN ('BOOKED', 'CHECKED_IN')
              AND (CAST(:lastProcessedId AS UUID) IS NULL OR id > :lastProcessedId)
            ORDER BY id ASC
            LIMIT :limit
        
        """)
    Set<AppointmentEntity> getUnAttendedAppointments(@Param("appointmentDate") LocalDate appointmentDate, @Param("lastProcessedId") UUID lastProcessedId, @Param("limit") int limit);

    @Query("""
            SELECT
            id,
            slot_id,
            branch_id,
            customer_username,
            service_type,
            status,
            reference,
            date_time,
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
            reschedule_count,
            COUNT(*) OVER () AS total_appointments_count
            FROM appointment
            WHERE customer_username = :customerUsername
            AND (CAST(:status AS VARCHAR) IS NULL OR status = :status)
            ORDER BY date_time DESC
            OFFSET :offset LIMIT :limit
        """)
    Set<AppointmentEntity> findByCustomerUsername(
            @Param("customerUsername") String customerUsername,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
}
