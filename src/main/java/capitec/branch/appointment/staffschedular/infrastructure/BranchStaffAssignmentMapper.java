package capitec.branch.appointment.staffschedular.infrastructure;

import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignment;
import capitec.branch.appointment.staffschedular.domain.StaffRef;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BranchStaffAssignmentMapper {

    /**
     * Converts  BranchStaffAssignmentEntity (database rows) into the structured
     */
    public BranchStaffAssignment toDomain(Set<BranchStaffAssignmentEntity> entities, String branchId) {
        if (entities == null || entities.isEmpty()) {
            // Initialize with an empty map if no records are found
            return new BranchStaffAssignment(branchId, new HashMap<>());
        }

        Map<LocalDate, Set<StaffRef>> weeklyStaff = entities.stream()
                // Group the flat list by the LocalDate (the 'day' field)
                .collect(Collectors.groupingBy(
                        BranchStaffAssignmentEntity::day,
                        // Within each day, collect the StaffRef components (username) into a Set
                        Collectors.mapping(
                                entity -> new StaffRef(entity.username()),
                                Collectors.toSet()
                        )
                ));

        return new BranchStaffAssignment(branchId, weeklyStaff);
    }


    /**
     * BranchStaffAssignment domain model into a list of
     */
    public Set<BranchStaffAssignmentEntity> toBranchStaffAssignmentEntity(BranchStaffAssignment domain) {
        
        // This is a list to hold all the flat row entities
        Set<BranchStaffAssignmentEntity> flatEntities = domain.getWeeklyStaff()
                .entrySet()
                .stream()
                // Stream over each day (LocalDate) in the map
                .flatMap(dayEntry -> {
                    LocalDate day = dayEntry.getKey();
                    Set<StaffRef> staffRefs = dayEntry.getValue();

                    // Map each StaffRef within the day's Set to a flat Entity record
                    return staffRefs.stream().map(staffRef -> new BranchStaffAssignmentEntity(
                            null,
                            staffRef.username(),
                            domain.getBranchId(), // Use the branchId from the Domain object
                            day
                    ));
                })
                .collect(Collectors.toSet());

        return flatEntities;
    }

}