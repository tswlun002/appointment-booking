package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Holiday;

import java.util.Set;

public interface HolidayClient {

    Set<Holiday> getHolidays(String countryCode , int year);
}
