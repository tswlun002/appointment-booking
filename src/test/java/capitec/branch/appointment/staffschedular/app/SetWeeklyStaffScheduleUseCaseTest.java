package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.staffschedular.domain.StaffRef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        
        // --- Branch 1:
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

        // ACT & ASSERT: No exception thrown means successful addition
        Assertions.assertDoesNotThrow(() ->
            setWeeklyStaffScheduleUseCase.execute(branchId1, schedule)
        );
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

        // ASSERT: No exception thrown means successful update
        Assertions.assertDoesNotThrow(() ->
            setWeeklyStaffScheduleUseCase.execute(branchId, updatedSchedule)
        );
    }
}