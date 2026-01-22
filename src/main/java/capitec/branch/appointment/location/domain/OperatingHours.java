package capitec.branch.appointment.location.domain;

public record OperatingHours(

        OperationTime weekdayHours,
        OperationTime saturdayHours,
        OperationTime sundayHours,
        OperationTime publicHolidayHours
) {


    public boolean isOpenOnSaturdays() {
        return saturdayHours != null && !saturdayHours.closed();
    }

    public boolean isOpenOnSundays() {
        return sundayHours != null && !sundayHours.closed();
    }

    public boolean isOpenOnPublicHolidays() {
        return publicHolidayHours != null && !publicHolidayHours.closed();
    }
}

