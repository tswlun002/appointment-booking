package capitec.branch.appointment.branch.infrastructure;


import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

@Repository
interface BranchRepository extends CrudRepository<BranchEntity, Long> {
    @Modifying
    @Transactional
    @Query("""
            WITH branch_lookup AS (
                SELECT id, branch_id AS business_id 
                FROM branch
                WHERE branch_id = :branchId
            )
            INSERT INTO branch_appointment_info
                (branch_id, branch_business_id, branch_key, slot_duration, utilization_factor,staff_count, day_type)
            SELECT
                b.id,                                   
                b.business_id,                          
                :dayType,
                :slotDuration,
                :utilizationFactor,
                :staffCount,
                :dayType
            FROM branch_lookup b
            ON CONFLICT (branch_id, day_type)
            DO UPDATE SET
                slot_duration = EXCLUDED.slot_duration,
                utilization_factor = EXCLUDED.utilization_factor
            """)
    int addBranchAppointmentConfigInfo(@Param("branchId") String branchId, @Param("slotDuration") int slotDuration,
                                       @Param("utilizationFactor") double utilizationFactor,@Param("staffCount") int staffCount, @Param("dayType") String dayType);


    @Query(value = """
                        SELECT 
                            u.id,
                            u.branch_id,
                            u.open_time,
                            u.close_time,
                            u.created_at,
                            u.last_modified_date,
                            a.branch_id AS address_branch_id,
                            a.created_at AS address_created_at,
                            a.last_modified_date AS address_last_modified_date,
                            a.street_number AS address_street_number,
                            a.street_name AS address_street_name,
                            a.suburb AS address_suburb,
                            a.city AS address_city,
                            a.province AS address_province,
                            a.postal_code AS address_postal_code,
                            a.country AS address_country,
                            ba.day_type AS ba_day_type,
                            ba.slot_duration AS ba_slot_duration,
                            ba.utilization_factor AS ba_utilization_factor,
                            ba.staff_count AS ba_staff_count
                            FROM branch AS u
                            INNER JOIN address AS a ON a.branch_id=u.id
                            LEFT JOIN branch_appointment_info AS ba  ON ba.branch_id=u.id
                       WHERE u.branch_id=:branchId 
            """)
    Optional<BranchEntity> getByBranchId(@Param("branchId") String branchId);

    @Modifying
    @Transactional
    @Query("""
            DELETE  FROM branch AS b WHERE  b.branch_id=:branchId
           """)
    int deletBranch(@Param("branchId") String branchId);

    @Query("""
            SELECT 
                u.id,
                u.branch_id,
                u.open_time,
                u.close_time,
                u.created_at,
                u.last_modified_date,
                a.branch_id AS address_branch_id,
                a.created_at AS address_created_at,
                a.last_modified_date AS address_last_modified_date,
                a.street_number AS address_street_number,
                a.street_name AS address_street_name,
                a.suburb AS address_suburb,
                a.city AS address_city,
                a.province AS address_province,
                a.postal_code AS address_postal_code,
                a.country AS address_country,
                ba.day_type AS ba_day_type,
                ba.slot_duration AS ba_slot_duration,
                ba.utilization_factor AS ba_utilization_factor,
                ba.staff_count AS ba_staff_count
                FROM branch AS u
                INNER JOIN address AS a ON a.branch_id=u.id
                LEFT JOIN branch_appointment_info AS ba  ON ba.branch_id=u.id
            """)
    Collection<BranchEntity> getAllBranch();
}
