package capitec.branch.appointment.staffschedular.domain;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.*;

public class BranchStaffAssignment {

    @NotBlank
    private final String branchId;
    @NotNull
    private Map<LocalDate, Set<StaffRef>> weeklyStaff;



    public BranchStaffAssignment(String branchId, Map<LocalDate, Set<StaffRef>> weeklyStaff) {
        this.branchId = branchId;
        this.weeklyStaff = weeklyStaff;
    }
    public String getBranchId() {
        return branchId;
    }

    public Map<LocalDate, Set<StaffRef>> getWeeklyStaff() {
        if (weeklyStaff == null) {
            return Collections.emptyMap();
        }
        Map<LocalDate, Set<StaffRef>> copy = new HashMap<>();
        weeklyStaff.forEach((date, refs) ->
                copy.put(date, Set.copyOf(refs))
        );
        return Collections.unmodifiableMap(copy);
    }

    public void setWeeklyStaff(Map<LocalDate, Set<StaffRef>> newWeeklyStaff) {

        if (this.weeklyStaff == null) {
            this.weeklyStaff = new HashMap<>(newWeeklyStaff);
        }
        else {

            for(var day : newWeeklyStaff.keySet()) {

                Set<StaffRef> staffRefs = this.weeklyStaff.get(day);

                if(staffRefs == null) {

                    staffRefs = newWeeklyStaff.get(day);
                }
                else {

                    staffRefs.addAll(newWeeklyStaff.get(day));
                }
                this.weeklyStaff.put(day, staffRefs);
            }
        }

    }

    public void addStaff(@NotNull LocalDate day, @NotNull Set<StaffRef> staff) {
        if (this.weeklyStaff == null) {
            this.weeklyStaff = new HashMap<>();

            this.weeklyStaff.put(day, staff);
            return;
        }

        Set<StaffRef> dailyStaff = this.weeklyStaff.computeIfAbsent(day, k -> new HashSet<>());
        dailyStaff.addAll(staff);

        this.weeklyStaff.put(day, dailyStaff);
    }

    public void addStaff(@NotNull LocalDate day, @NotNull StaffRef staff) {
        if (this.weeklyStaff == null) {
            this.weeklyStaff = new HashMap<>();
            Set<StaffRef> newStaff = new HashSet<>();
            newStaff.add(staff);
            this.weeklyStaff.put(day, newStaff);
            return;
        }

        Set<StaffRef> dailyStaff = this.weeklyStaff.get(day);

        if (dailyStaff == null) {
            dailyStaff = new HashSet<>();
        }
        if (dailyStaff.contains(staff)) {

            throw new EntityAlreadyExistException(String.format("Staff(%s) already scheduled to work at the given day(%s).",staff.username(), day.toString()));
        }
        dailyStaff.add(staff);

        this.weeklyStaff.put(day, dailyStaff);
    }


    public void removeStaff(@NotNull LocalDate day, @NotNull StaffRef staff) {

        if (this.weeklyStaff == null) {
            throw new IllegalArgumentException("Cannot remove a staff that has no weekly staff");
        }

        Set<StaffRef> dailyStaff = this.weeklyStaff.get(day);

        if (dailyStaff == null || dailyStaff.isEmpty()) {

            throw new IllegalArgumentException("Cannot remove a staff that has no daily staff");
        }
        if (!dailyStaff.contains(staff)) {

            throw new NoSuchElementException("Cannot remove a staff that does not exist on working day: " + day.toString());
        }
        dailyStaff.remove(staff);

        this.weeklyStaff.put(day, dailyStaff);
    }

}
