package capitec.branch.appointment.utils.sharekernel.day.app;

import capitec.branch.appointment.utils.sharekernel.day.domain.Day;
import capitec.branch.appointment.utils.sharekernel.day.domain.Scale;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
@UseCase
@RequiredArgsConstructor
@Validated
public class GetDateOfNextDaysQuery {

    private final CheckHolidayQuery checkHolidayQuery;

    public Set<Day> execute(DayOfWeek fromDay, DayOfWeek toDay, Scale scale) {
        if(scale.getValue() ==1){
            return execute(fromDay, toDay);
        }

        LocalDate date = LocalDate.now();
        Set<Day> days = new HashSet<>();
       for(var i = 0; i< scale.getValue(); i++) {

           if (date.getDayOfWeek().equals(fromDay) || !days.isEmpty()) {

               boolean isHoliday = checkHolidayQuery.execute(date);
               Day day = new Day(date.getDayOfWeek(), date);
               day.setIsHoliday(isHoliday);
               days.add(day);

           }
           date = date.plusDays(1);
       }
       return days;

    }
    public Set<Day> execute(DayOfWeek fromDay, DayOfWeek toDay) {

        LocalDate date = LocalDate.now();
        Set<Day> days = new HashSet<>();
       while(true) {

           if (date.getDayOfWeek().equals(fromDay) || !days.isEmpty()) {

               boolean isHoliday = checkHolidayQuery.execute(date);
               Day day = new Day(date.getDayOfWeek(), date);
               day.setIsHoliday(isHoliday);
               days.add(day);

               if (date.getDayOfWeek().equals(toDay)) {
                   break;
               }

           }
           date = date.plusDays(1);
       }
       return days;

    }
    public Set<Day> execute(LocalDate fromDay, LocalDate toDay) {
        Assert.notNull(fromDay, "The fromDay must not be null");
        Assert.notNull(toDay, "The toDay must not be null");
        LocalDate date = fromDay;
        Set<Day> days = new HashSet<>();
        while(true) {

            boolean isHoliday = checkHolidayQuery.execute(date);
            Day day = new Day(date.getDayOfWeek(), date);
            day.setIsHoliday(isHoliday);
            days.add(day);

            if (date.equals(toDay)) {
                break;
            }

            date = date.plusDays(1);
        }
        return days;

    }
    public Day execute( LocalDate day) {
        Assert.notNull(day, "The day must not be null");
        boolean isHoliday = checkHolidayQuery.execute(day);
        Day day1 = new Day(day.getDayOfWeek(), day);
        day1.setIsHoliday(isHoliday);

        return day1;

    }
}
