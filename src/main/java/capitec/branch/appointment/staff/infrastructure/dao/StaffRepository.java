package capitec.branch.appointment.staff.infrastructure.dao;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Repository
interface StaffRepository extends CrudRepository<StaffEntity, Long> {
    @Transactional
    @Query(value = """
       UPDATE staff SET  status=:status, last_modified_date=CURRENT_TIMESTAMP WHERE  staff.username=:username
       RETURNING id, username, branch_id, status, created_at, last_modified_date
""")
    Optional<StaffEntity> updatedStaffStatus(@Param("username") String username, @Param("status") String status);


    @Query("""
         SELECT  id, username, branch_id, status, created_at, last_modified_date FROM staff AS s
            WHERE s.branch_id=:branchId AND s.status=:status
    """)
    Set<StaffEntity> getStaffByBranchIdAndStatus(@Param("branchId") String branchId, @Param("status") String status);
     @Modifying
    @Query("""
            DELETE FROM  staff AS s WHERE s.username=:username
          """)
    int deleteStaffByUsername(@Param("username") String username);
}
