package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.HolidayClient;
import capitec.branch.appointment.slots.domain.Holiday;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@UseCase
@Validated
@RequiredArgsConstructor
public class CheckHolidayQuery {

    private final static String COUNTRY_CODE = "ZA";
    private final static int CURRENT_YEAR = LocalDateTime.now().getYear();
    private final HolidayClient holidayClient;

    /**
     * Checks if a given dateOfSlots is a public holiday in the specified country.
     * @param date The dateOfSlots to check.
     * @return True if the dateOfSlots is a holiday, false otherwise.
     */
    public boolean execute(LocalDate date) {

        Set<Holiday> holidays = holidayClient.getHolidays(COUNTRY_CODE, CURRENT_YEAR);

        return holidays.stream()
                .map(Holiday::date)
                .anyMatch(date::equals);
    }
}