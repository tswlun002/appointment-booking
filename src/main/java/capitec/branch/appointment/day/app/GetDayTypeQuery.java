package capitec.branch.appointment.day.app;

import capitec.branch.appointment.day.domain.DayType;
import capitec.branch.appointment.day.domain.Holiday;
import capitec.branch.appointment.day.domain.HolidayClient;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static capitec.branch.appointment.day.domain.Day.isWeekend;

@UseCase
@Validated
@RequiredArgsConstructor
public class GetDayTypeQuery {
   private final CheckHolidayQuery checkHolidayQuery;


    /**
     * Return type of today's day in South Africa
     *
     * @param day is the day to check day type of
     * @return day type of today
     * @throws   IllegalArgumentException if the day to check is null
     */
    public DayType execute(LocalDate day) {
        if (day == null) {
            throw  new IllegalArgumentException("The day to check must not be null");
        }
        return checkHolidayQuery.execute(day)?
                DayType.HOLIDAY:
                isWeekend(day.getDayOfWeek())?
                DayType.WEEKEND:DayType.WEEK_DAYS;
    }
}