package capitec.branch.appointment.sharekernel.day.app;

import capitec.branch.appointment.sharekernel.day.domain.Holiday;

import java.util.Set;

public interface HolidayClient {

    Set<Holiday> getHolidays(String countryCode , int year);
}
