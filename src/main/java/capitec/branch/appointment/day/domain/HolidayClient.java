package capitec.branch.appointment.day.domain;

import java.util.Set;

public interface HolidayClient {

    Set<Holiday> getHolidays(String countryCode , int year);
}
