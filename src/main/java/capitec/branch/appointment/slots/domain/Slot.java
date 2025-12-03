package capitec.branch.appointment.slots.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class Slot {

    private final LocalDate day;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer number;
    private boolean isBooked;

    public Slot(LocalDate day, LocalTime startTime, LocalTime endTime, Integer number) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.number = number;
        this.isBooked = false;
    }

    public LocalDate getDay() {
        return day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Duration getDuration() {
        return Duration.ofMinutes(Duration.between(startTime, endTime).toMinutes());
    }

    public Integer getNumber() {
        return number;
    }

    public boolean isBooked() {
        return isBooked;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Slot slot)) return false;
        return isBooked == slot.isBooked && Objects.equals(day, slot.day) && Objects.equals(startTime, slot.startTime) && Objects.equals(endTime, slot.endTime) && Objects.equals(number, slot.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, startTime, endTime, number, isBooked);
    }

    @Override
    public String toString() {
        return "Slot{" +
                "day=" + day +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", number=" + number +
                ", isBooked=" + isBooked +
                '}';
    }
}
