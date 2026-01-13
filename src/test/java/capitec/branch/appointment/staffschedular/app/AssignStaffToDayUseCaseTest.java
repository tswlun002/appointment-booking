package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.staffschedular.domain.StaffRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignStaffToDayUseCaseTest extends StaffSchedulerTestBase {

    @Autowired
    private SetWeeklyStaffScheduleUseCase setWeeklyStaffScheduleUseCase; // For initial setup
    @Autowired
    private CancelFutureWorkingDaysUseCase cancelFutureWorkingDaysUseCase; // For tear down setup after each method run
    @Autowired
    private AssignStaffToDayUseCase assignStaffToDayUseCase;
    private String testBranchId;
    private LocalDate today;

    @BeforeEach
    void setupBranchAndDate() {
        testBranchId = branches.getFirst().getBranchId();
        today = LocalDate.now();
    }
    @AfterEach
    void cleanBranchAndDate() {
        cancelFutureWorkingDaysUseCase.execute(testBranchId, DayOfWeek.values());
    }


    /**
     * Test case mirroring the user's request:
     * 1. Set up a schedule with a base set of staff (staff[1] to staff[N]) for the week.
     * 2. Add the remaining staff member (staff[0]) to today's schedule.
     */
    @Test
    void testAssignStaffToParticularDay_SuccessfullyAddsNewStaff() {

        // SETUP: Create an initial schedule for the week excluding the first staff member
        var initialStaffToExclude = staff.getFirst();
        var staffInitiallyAssigned = staff.subList(1, staff.size());

        var schedule = new HashMap<LocalDate, Set<StaffRef>>();
        IntStream.range(0, 7).forEach(i -> {
            LocalDate date = today.plusDays(i);
            Set<StaffRef> initialRefs = staffInitiallyAssigned.stream()
                    .map(StaffRef::new)
                    .collect(Collectors.toSet());
            schedule.put(date, initialRefs);
        });

        // Ensure the initial schedule is present in the database
        setWeeklyStaffScheduleUseCase.execute(testBranchId, schedule);

        // Count staff before assignment for verification
        int initialStaffCount = staffInitiallyAssigned.size();

        // ACT: Add the excluded staff member (staff[0]) to TODAY
        BranchStaffAssignmentDTO assignmentDTO = new BranchStaffAssignmentDTO(initialStaffToExclude, today);
        boolean added = assignStaffToDayUseCase.execute(testBranchId, assignmentDTO);

        // ASSERT 1: The use case reported successful addition
        assertThat(added).isTrue();

//        // ASSERT 2: Verify the final staff count is initialCount + 1
//        BranchStaffAssignment assignment = originalStaffScheduleService.get(testBranchId, today).orElseThrow();
//        Set<StaffRef> finalStaffRefs = assignment.getWeeklyStaff().get(today);
//
//        assertThat(finalStaffRefs).hasSize(initialStaffCount + 1);
//
//        // ASSERT 3: Verify the newly added staff member is present
//        assertThat(finalStaffRefs.stream().map(StaffRef::username)).contains(initialStaffToExclude);
    }

    @Test
    void testAssignStaffToParticularDay_ThrowsExceptionIfStaffAlreadyExists() {

        // SETUP: Create a minimal schedule where all staff are assigned for today
        StaffRef staffToDuplicate = new StaffRef(staff.getFirst());
        var schedule = new HashMap<LocalDate, Set<StaffRef>>();
        schedule.put(today, Set.of(staffToDuplicate));
        setWeeklyStaffScheduleUseCase.execute(testBranchId, schedule);

        // ACT & ASSERT: Attempt to add the same staff member again
        BranchStaffAssignmentDTO duplicateDTO = new BranchStaffAssignmentDTO(staffToDuplicate.username(), today);

        assertThatThrownBy(() -> assignStaffToDayUseCase.execute(testBranchId, duplicateDTO))
                .hasCauseInstanceOf(EntityAlreadyExistException.class)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Staff already scheduled to work at the given day.");

//        // VERIFY: The count remains unchanged
//        BranchStaffAssignment assignment = originalStaffScheduleService.get(testBranchId, today).orElseThrow();
//        assertThat(assignment.getWeeklyStaff().get(today)).hasSize(1);
    }

    @Test
    void testAssignStaffToParticularDay_ThrowsNotFoundIfScheduleDoesNotExist() {

        // SETUP: Ensure no schedule exists for today's dateOfSlots (this is handled by the Base class tearDown)

        // ACT & ASSERT: Attempt to add staff
        BranchStaffAssignmentDTO assignmentDTO = new BranchStaffAssignmentDTO(staff.getFirst(), today);

        assertThatThrownBy(() -> assignStaffToDayUseCase.execute(testBranchId, assignmentDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Could not find branch staff schedular of the given day.");
    }
}