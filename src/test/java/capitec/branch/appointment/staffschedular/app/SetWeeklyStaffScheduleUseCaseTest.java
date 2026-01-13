package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.staffschedular.domain.StaffRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SetWeeklyStaffScheduleUseCaseTest extends StaffSchedulerTestBase {

    @Autowired
    private SetWeeklyStaffScheduleUseCase setWeeklyStaffScheduleUseCase;

    @Test
    void testSetWeeklyStaffSchedule_AddsNewSchedule() {

        var schedule = new HashMap<LocalDate, Set<StaffRef>>();
        LocalDate now = LocalDate.now();

        int totalStaff = staff.size();
        int numBranches = branches.size();
        // Calculate staff evenly split between the two test branches (5 staff total / 2 branches = 2.5, using 2)
        int numStaffPerBranch = totalStaff / numBranches; 
        
        // --- Branch 1: BR001 ---
        String branchId1 = branches.getFirst().getBranchId();
        int index1 = 0; // Staff 0, 1
        
        // Build the weekly schedule map for branch 1
        IntStream.range(0, 7).forEach(i -> {
            LocalDate date = now.plusDays(i);
            Set<StaffRef> staffForDay = staff
                    .subList(index1, index1 + numStaffPerBranch)
                    .stream().map(StaffRef::new)
                    .collect(Collectors.toSet());

            schedule.put(date, staffForDay);
        });

        // ACT
        boolean added = setWeeklyStaffScheduleUseCase.execute(branchId1, schedule);

        // ASSERT 1: The use case reported successful addition
        assertThat(added).isTrue();
        
   /*     // ASSERT 2: Verify the data was actually saved for today's dateOfSlots
        BranchStaffAssignment assignment = originalStaffScheduleService.get(branchId1, now).orElse(null);
        assertThat(assignment).isNotNull();
        // Get the list of staff assigned to today
        Set<String> assignedUsernames = assignment.getWeeklyStaff().get(now).stream().map(StaffRef::username).collect(Collectors.toSet());
        
        // Verify the bookingCount of staff saved matches the bookingCount assigned
        assertThat(assignedUsernames).hasSize(numStaffPerBranch);
        
        // Verify the correct staff members were assigned
        assertThat(assignedUsernames).containsAll(staffUsernames.subList(index1, index1 + numStaffPerBranch));*/
    }
    
    @Test
    void testSetWeeklyStaffSchedule_UpdatesExistingSchedule() {
        
        // SETUP: Add a schedule first
        var initialSchedule = new HashMap<LocalDate, Set<StaffRef>>();
        LocalDate today = LocalDate.now();
        String branchId = branches.getFirst().getBranchId();
        
        // Assign only staff user 0 for the whole week
        StaffRef initialStaff = new StaffRef(staff.getFirst());
        IntStream.range(0, 7).forEach(i -> initialSchedule.put(today.plusDays(i), Set.of(initialStaff)));
        setWeeklyStaffScheduleUseCase.execute(branchId, initialSchedule);
        
        // ACT: Overwrite the schedule with new data (staff user 1 and 2 for the whole week)
        var updatedSchedule = new HashMap<LocalDate, Set<StaffRef>>();
        Set<StaffRef> newStaff = staff.subList(1, 3).stream().map(StaffRef::new).collect(Collectors.toSet());
        IntStream.range(0, 7).forEach(i -> updatedSchedule.put(today.plusDays(i), newStaff));

        boolean updated = setWeeklyStaffScheduleUseCase.execute(branchId, updatedSchedule);

        // ASSERT 1: The use case reported successful update (or add)
        assertThat(updated).isTrue();
        
//        // ASSERT 2: Verify the data was overwritten for today's dateOfSlots
//        BranchStaffAssignment assignment = originalStaffScheduleService.get(branchId, today).orElseThrow();
//        Set<String> assignedUsernames = assignment.getWeeklyStaff().get(today).stream().map(StaffRef::username).collect(Collectors.toSet());
//
//        // Verify the bookingCount of staff saved matches the new assignment (2 staff)
//        assertThat(assignedUsernames).hasSize(2);
//
//        // Verify the correct NEW staff members were assigned
//        assertThat(assignedUsernames).contains(staffUsernames.get(1), staffUsernames.get(2));
//
//        // Verify the OLD staff member is gone
//        assertThat(assignedUsernames).doesNotContain(staffUsernames.getFirst());
    }
}