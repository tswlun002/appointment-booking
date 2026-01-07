package capitec.branch.appointment.slots.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public class Slot {

    public static String ID_FIELD_NAME = "id";
    public static String STATUS_FIELD_NAME = "status";
    public static String VERSION_FIELD_NAME = "version";

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
    private int version;

    public Slot(LocalDate day, LocalTime startTime, LocalTime endTime, Integer number, String branchId) {
        this.id =  UUID.randomUUID();
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.number = number;
        this.branchId = branchId;
        this.status = SlotStatus.AVAILABLE;
        this.version = 0;
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Slot start time (" + startTime + ") must be strictly before the end time (" + endTime + ").");
        }
    }

    public void book(LocalDateTime currentTime){
        validateSlotTimeNotPassed(currentTime);
        if(status == SlotStatus.BLOCKED){
            throw new IllegalStateException("Cannot book a blocked slot.");
        }
        if(status == SlotStatus.BOOKED){
            throw new IllegalStateException("Slot is already booked.");
        }
        this.status = SlotStatus.BOOKED;
       // increaseVersion();
    }
    public void release(LocalDateTime currentTime){
        validateSlotTimeNotPassed(currentTime);
        if(status == SlotStatus.AVAILABLE){
            throw new IllegalStateException("Slot is already available.");
        }
        this.status = SlotStatus.AVAILABLE;
        //increaseVersion();

    }
    public void  expire(){
        if(status == SlotStatus.AVAILABLE || status == SlotStatus.BLOCKED){
            this.status = SlotStatus.EXPIRED;
            //increaseVersion();
        }
    }
    public void block(LocalDateTime currentTime){
        validateSlotTimeNotPassed(currentTime);
        if(status == SlotStatus.BLOCKED){
            throw new IllegalStateException("Cannot block a blocked slot.");
        }
        this.status = SlotStatus.BLOCKED;
        //increaseVersion();
    }

    private void validateSlotTimeNotPassed(LocalDateTime currentTime){
        LocalDateTime localDateTime = LocalDateTime.of(day, startTime);
        if(currentTime.isBefore(localDateTime)){
            throw new IllegalStateException("Cannot modify a slot that has already started.");
        }
    }

    private   void increaseVersion(){
        this.version++;
    }


    public UUID getId() {
        return id;
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
    public int getVersion() {
        return version;
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
                ", branchId='" + branchId +
                ", status=" + status +
                ", version=" + version +
                '}';
    }
}
