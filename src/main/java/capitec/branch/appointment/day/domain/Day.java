package capitec.branch.appointment.day.domain;

import java.time.DayOfWeek;

public class Day {

    public static boolean isWeekend(DayOfWeek date) {

        return date== DayOfWeek.SATURDAY ||
                date == DayOfWeek.SUNDAY;
    }
}
