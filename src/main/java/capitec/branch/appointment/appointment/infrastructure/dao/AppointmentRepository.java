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
            reschedule_count,
            1 AS total_appointments_count
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
            reschedule_count,
            1 AS total_appointments_count
        FROM
        appointment
        WHERE  id = :appointmentId
    """)
    Optional<AppointmentEntity> getUserActiveAppointment(@Param("appointmentId")UUID appointmentId);


    @Query("""
            SELECT 
            a.id,
            a.slot_id,
            a.branch_id,
            a.customer_username,
            a.service_type,
            a.status,
            a.reference,
            a.date_time,
            a.version,
            a.created_at,
            a.updated_at,
            a.checked_in_at,
            a.in_progress_at,
            a.completed_at,
            a.terminated_at,
            a.terminated_by,
            a.termination_reason,
            a.termination_notes,
            a.assigned_consultant_id,
            a.service_notes,
            a.previous_slot_id,
            a.reschedule_count,
            COUNT(*) OVER () AS total_appointments_count
            FROM appointment AS a INNER JOIN  slot AS s ON s.id = a.slot_id
                AND a.status IN ('BOOKED', 'CHECKED_IN')
                AND ( s.day + s.end_time) < NOW()
                AND DATE(a.date_time) >= :appointmentDate
                AND (CAST(:lastProcessedId AS UUID) IS NULL OR a.id > :lastProcessedId)
            ORDER BY a.id ASC
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
