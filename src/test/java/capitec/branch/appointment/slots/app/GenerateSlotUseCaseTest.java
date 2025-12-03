package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.slots.domain.Slot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


class GenerateSlotUseCaseTest extends AppointmentBookingApplicationTests {


   @Autowired
    private GenerateSlotUseCase generateSlotUseCase;

   @Test
    public void generateSlot() {

        wireMockGetHolidayByYearAndCountryCode("2025", "ZA");

       generateSlotUseCase.createNext7DaySlots();

       Map<LocalDate, List<Slot>> weeklySlots = generateSlotUseCase.next7DaySlots(LocalDate.now());
       assertThat(weeklySlots).isNotEmpty();
       assertThat(weeklySlots.size()).isEqualTo(7);

       List<LocalDate> holidays = weeklySlots.keySet()
               .stream().filter(d ->generateSlotUseCase.isHoliday(d))
               .toList();

       for (LocalDate day : holidays) {
           List<Slot> weekDaySlots = weeklySlots.get(day);
           assertThat(weekDaySlots).isEmpty();
       }

       List<LocalDate> weekDays = weeklySlots.keySet().stream()
               .filter(d -> !(generateSlotUseCase.isHoliday(d) || GenerateSlotUseCase.isWeekend(d.getDayOfWeek())))
               .toList();

       for (LocalDate day : weekDays) {
           List<Slot> weekDaySlots = weeklySlots.get(day);
           assertThat(weekDaySlots).isNotEmpty();
           assertThat(weekDaySlots.size()).isEqualTo(9);
       }
       List<LocalDate> weekEnd= weeklySlots.keySet().stream()
               .filter(d -> GenerateSlotUseCase.isWeekend(d.getDayOfWeek()))
               .toList();

       for (var day : weekEnd ) {
           List<Slot> weekDaySlots = weeklySlots.get(day);
           assertThat(weekDaySlots).isNotEmpty();
           assertThat(weekDaySlots.size()).isEqualTo(6);
       }


   }
}