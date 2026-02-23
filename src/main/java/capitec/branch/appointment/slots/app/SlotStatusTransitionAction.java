package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Slot;

import java.time.LocalDateTime;
import java.util.UUID;

public  sealed interface   SlotStatusTransitionAction  permits SlotStatusTransitionAction.Book,
        SlotStatusTransitionAction.Block,SlotStatusTransitionAction.Release, SlotStatusTransitionAction.Expire  {
    UUID  getId();
    Slot execute(Slot slot);

    record Book(UUID id,LocalDateTime currentTime) implements SlotStatusTransitionAction {
        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public Slot execute(Slot slot) {
            slot.book(currentTime);
            return slot;
        }
    }

    record Block(UUID id,LocalDateTime currentTime) implements SlotStatusTransitionAction {
        @Override
        public UUID getId() {
            return  id;
        }

        @Override
        public Slot execute(Slot slot) {
            slot.block(currentTime);
            return slot;
        }
    }

    record Release(UUID id,LocalDateTime currentTime) implements SlotStatusTransitionAction {
        @Override
        public UUID getId() {
            return  id;
        }

        @Override
        public Slot execute(Slot slot) {
            slot.release(currentTime);
            return slot;
        }
    }
    record Expire(UUID id) implements SlotStatusTransitionAction {
        @Override
        public UUID getId() {
            return  id;
        }

        @Override
        public Slot execute(Slot slot) {
            slot.expire();
            return slot;
        }
    }

}
