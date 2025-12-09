package capitec.branch.appointment.branch.infrastructure;


import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
interface BranchRepository extends CrudRepository<BranchEntity, Long> {
    @Modifying
    @Transactional
    @Query("""
            INSERT INTO branch_appointment_info (branch_id, branch_key, slot_duration, utilization_factor, day_type)
            VALUES (:branchId, :dayType, :slotDuration, :utilizationFactor, :dayType)
            ON CONFLICT (branch_id, day_type) 
            DO UPDATE SET 
                slot_duration = EXCLUDED.slot_duration,
                utilization_factor = EXCLUDED.utilization_factor
            """)
    int addBranchAppointmentConfigInfo(@Param("branchId") String branchId, @Param("slotDuration") int slotDuration,
                                       @Param("utilizationFactor") double utilizationFactor, @Param("dayType") String dayType);


    @Query(value = """
                        SELECT * FROM branch AS U 
                            INNER JOIN address AS a ON a.branch_id=u.branch_id
                            LEFT JOIN branch_appointment_info AS ba  ON ba.branch_id=u.branch_id
                            LEFT JOIN branch_staff_assignment AS ss ON ss.branch_id=u.branch_id
                       WHERE U.branch_id=:branchId 
            """)
    Optional<BranchEntity> getBranchById(@Param("branchId") String branchId);
}
