package capitec.branch.appointment.staffschedular.infrastructure;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Repository
interface BranchStaffAssignmentRepository extends CrudRepository<BranchStaffAssignmentEntity, Long> {

    @Modifying
    @Query("""
            INSERT INTO branch_staff_assignment (branch_id, username, day)
            SELECT * FROM UNNEST(:branchIds, :usernames, :days) 
            ON CONFLICT  DO NOTHING
            """)
    int bulkInsertOrIgnore(
            @Param("branchIds") String[] branchIds,
            @Param("usernames") String[]usernames,
            @Param("days") LocalDate[] days
    );

    default int bulkUpsertAssignments(Set<BranchStaffAssignmentEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        String[] branchIds = entities.stream().map(BranchStaffAssignmentEntity::branchId).toArray(String[]::new);
        String[]  usernames = entities.stream().map(BranchStaffAssignmentEntity::username).toArray(String[]::new);
        LocalDate[] days = entities.stream().map(BranchStaffAssignmentEntity::day).toArray(LocalDate[]::new);

        return bulkInsertOrIgnore(
                branchIds,
                usernames,
                days
        );
    }

    @Query("""
            SELECT b.username,b.branch_id,b.day FROM branch_staff_assignment AS B
            WHERE b.branch_id=:branchId AND (CAST(:day AS DATE) IS NULL OR b.day=:day)
            """)
    Set<BranchStaffAssignmentEntity> getWorkingStaff(@Param("branchId") String branchId, @Param("day") LocalDate day);
    @Transactional
    @Modifying
    @Query("""
        DELETE FROM branch_staff_assignment AS ba 
               WHERE ba.branch_id=:branchId 
               AND ba.day IN (:days)
        """)
    int cancelWorkingDay(@Param("branchId") String branchId, @Param("days") Set<LocalDate> days);
}
