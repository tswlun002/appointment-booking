package capitec.branch.appointment.sharekernel.day.domain;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

public enum Scale{
    DAY(1),
    WEEK(7),
    MONTH(YearMonth.of(LocalDate.now().getYear(), LocalDate.now().getMonth()).lengthOfMonth()),
    YEAR(Year.of(LocalDate.now().getYear()).isLeap()?366:365),
    DECADE((Year.of(LocalDate.now().getYear()).isLeap()?366:365)*10),
    CENTURY((Year.of(LocalDate.now().getYear()).isLeap()?366:365)*100);

    private final int value;

    Scale(int i) {
        value = i;
    }


    public int getValue() {
        return value;
    }
}
