package capitec.branch.appointment.slots.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public class Slot {
    @NotNull
    private final UUID id;
    @NotNull(message = "Day cannot be null")
    private final LocalDate day;

    @NotNull(message = "Start time cannot be null")
    private final LocalTime startTime;

    @NotNull(message = "End time cannot be null")
    private final LocalTime endTime;

    @NotNull(message = "Number cannot be null")
    @PositiveOrZero(message = "Number must be zero or positive")
    private final Integer number;

    @NotBlank(message = "Branch ID cannot be blank")
    private final String branchId;

    @NotNull(message = "Status cannot be null")
    private SlotStatus status;

    public Slot(LocalDate day, LocalTime startTime, LocalTime endTime, Integer number, String branchId) {
        this.id =  UUID.randomUUID();
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.number = number;
        this.branchId = branchId;
        this.status = SlotStatus.AVAILABLE;
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Slot start time (" + startTime + ") must be strictly before the end time (" + endTime + ").");
        }
    }

    public void book(){
        if(status != SlotStatus.AVAILABLE){
            throw new IllegalStateException("Slot is not available for booking.");
        }
        this.status = SlotStatus.BOOKED;
    }
    public  boolean isAvailable(){
        return status == SlotStatus.AVAILABLE;
    }
    public void release(){
        if(status == SlotStatus.AVAILABLE){
            return;
        }
        this.status = SlotStatus.AVAILABLE;

    }


    public UUID getId() {
        return id;
    }

    public void setStatus(@NotNull SlotStatus status) {
        this.status = status;
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

    public SlotStatus getStatus() {
        return status;
    }
    public String getBranchId() {
        return branchId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Slot slot)) return false;
        return Objects.equals(id, slot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Slot{" +
                "id=" + id +
                ", day=" + day +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", number=" + number +
                ", branchId='" + branchId + '\'' +
                ", status=" + status +
                '}';
    }
}
