package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.SlotDayType;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

import static capitec.branch.appointment.slots.domain.Day.isWeekend;

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
    public SlotDayType execute(LocalDate day) {
        if (day == null) {
            throw  new IllegalArgumentException("The day to check must not be null");
        }
        return checkHolidayQuery.execute(day)?
                SlotDayType.HOLIDAY:
                isWeekend(day.getDayOfWeek())?
                SlotDayType.WEEKEND: SlotDayType.WEEK_DAYS;
    }
}