package capitec.branch.appointment.day.domain;

import org.springframework.util.Assert;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

public class Day{

        private final DayOfWeek value;
        private final LocalDate date;
        private Boolean isHoliday;

    public Day(DayOfWeek value, LocalDate date) {
        Assert.notNull(value, "The value must not be null");
        Assert.notNull(date, "The date must not be null");
        this.value = value;
        this.date = date;
    }

    public  void setIsHoliday(Boolean isHoliday) {
        Assert.notNull(isHoliday, "The isHoliday must not be null");
        this.isHoliday = isHoliday;
    }


    public boolean isWeekday() {
        return switch (value){
            case SATURDAY,SUNDAY -> false;
            case MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY -> true;
        };
    }

    public DayOfWeek getValue() {
        return value;
    }

    public LocalDate getDate() {
        return date;
    }

    public Boolean isHoliday() {
        return isHoliday;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Day day)) return false;
        return Objects.equals(date, day.date);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(date);
    }

    @Override
    public String toString() {
        return "Day{" +
                "value=" + value +
                ", date=" + date +
                ", isHoliday=" + isHoliday +
                '}';
    }
}
