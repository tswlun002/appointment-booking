package capitec.branch.appointment.slots.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(9, 30);
    private static final Integer MAX_CAPACITY = 3;
    private static final String BRANCH_ID = "BR001";

    private Slot slot;
    private LocalDateTime beforeSlotTime;

    @BeforeEach
    void setUp() {
        slot = new Slot(TOMORROW, START_TIME, END_TIME, MAX_CAPACITY, BRANCH_ID);
        beforeSlotTime = LocalDateTime.of(TOMORROW, START_TIME.minusHours(1));
    }

    @Nested
    class Construction {

        @Test
        void shouldCreateSlotWithValidParameters() {
            assertThat(slot.getId()).isNotNull();
            assertThat(slot.getDay()).isEqualTo(TOMORROW);
            assertThat(slot.getStartTime()).isEqualTo(START_TIME);
            assertThat(slot.getEndTime()).isEqualTo(END_TIME);
            assertThat(slot.getMaxBookingCapacity()).isEqualTo(MAX_CAPACITY);
            assertThat(slot.getBookingCount()).isZero();
            assertThat(slot.getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
            assertThat(slot.getVersion()).isZero();
        }

        @Test
        void shouldThrowExceptionWhenStartTimeIsAfterEndTime() {
            assertThatThrownBy(() -> new Slot(TOMORROW, END_TIME, START_TIME, MAX_CAPACITY, BRANCH_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be strictly before");
        }

        @Test
        void shouldThrowExceptionWhenStartTimeEqualsEndTime() {
            assertThatThrownBy(() -> new Slot(TOMORROW, START_TIME, START_TIME, MAX_CAPACITY, BRANCH_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be strictly before");
        }

        @Test
        void shouldThrowExceptionWhenMaxCapacityIsZero() {
            assertThatThrownBy(() -> new Slot(TOMORROW, START_TIME, END_TIME, 0, BRANCH_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Max capacity must be positive");
        }

        @Test
        void shouldThrowExceptionWhenMaxCapacityIsNegative() {
            assertThatThrownBy(() -> new Slot(TOMORROW, START_TIME, END_TIME, -1, BRANCH_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Max capacity must be positive");
        }
    }

    @Nested
    class Book {

        @Test
        void shouldIncrementBookingCount() {
            slot.book(beforeSlotTime);

            assertThat(slot.getBookingCount()).isEqualTo(1);
            assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        }

        @Test
        void shouldTransitionToBookedWhenCapacityReached() {
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);

            assertThat(slot.getBookingCount()).isEqualTo(MAX_CAPACITY);
            assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        }

        @Test
        void shouldThrowExceptionWhenSlotIsFullyBooked() {
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);

            assertThatThrownBy(() -> slot.book(beforeSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fully booked");
        }

        @Test
        void shouldThrowExceptionWhenSlotIsBlocked() {
            slot.block(beforeSlotTime);

            assertThatThrownBy(() -> slot.book(beforeSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("blocked");
        }

        @Test
        void shouldThrowExceptionWhenSlotIsExpired() {
            slot.expire();

            assertThatThrownBy(() -> slot.book(beforeSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        void shouldThrowExceptionWhenSlotTimeHasPassed() {
            LocalDateTime afterSlotTime = LocalDateTime.of(TOMORROW, START_TIME.plusMinutes(1));

            assertThatThrownBy(() -> slot.book(afterSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already started");
        }
    }

    @Nested
    class Release {

        @Test
        void shouldDecrementBookingCount() {
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);

            slot.release(beforeSlotTime);

            assertThat(slot.getBookingCount()).isEqualTo(1);
        }

        @Test
        void shouldTransitionToAvailableWhenReleasedFromBooked() {
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);
            assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);

            slot.release(beforeSlotTime);

            assertThat(slot.getBookingCount()).isEqualTo(2);
            assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        }

        @Test
        void shouldRemainBlockedWhenReleasedFromBlockedSlot() {
            slot.book(beforeSlotTime);
            slot.block(beforeSlotTime);

            slot.release(beforeSlotTime);

            assertThat(slot.getBookingCount()).isEqualTo(0);
            assertThat(slot.getStatus()).isEqualTo(SlotStatus.BLOCKED);
        }

        @Test
        void shouldThrowExceptionWhenNoBookingsToRelease() {
            assertThatThrownBy(() -> slot.release(beforeSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No bookings to release");
        }

        @Test
        void shouldThrowExceptionWhenSlotTimeHasPassed() {
            slot.book(beforeSlotTime);
            LocalDateTime afterSlotTime = LocalDateTime.of(TOMORROW, START_TIME.plusMinutes(1));

            assertThatThrownBy(() -> slot.release(afterSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already started");
        }
    }

    @Nested
    class Block {

        @Test
        void shouldBlockAvailableSlot() {
            slot.block(beforeSlotTime);

            assertThat(slot.getStatus()).isEqualTo(SlotStatus.BLOCKED);
        }

        @Test
        void shouldBlockBookedSlot() {
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);

            slot.block(beforeSlotTime);

            assertThat(slot.getStatus()).isEqualTo(SlotStatus.BLOCKED);
            assertThat(slot.getBookingCount()).isEqualTo(3);
        }

        @Test
        void shouldThrowExceptionWhenAlreadyBlocked() {
            slot.block(beforeSlotTime);

            assertThatThrownBy(() -> slot.block(beforeSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already blocked");
        }

        @Test
        void shouldThrowExceptionWhenSlotIsExpired() {
            slot.expire();

            assertThatThrownBy(() -> slot.block(beforeSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        void shouldThrowExceptionWhenSlotTimeHasPassed() {
            LocalDateTime afterSlotTime = LocalDateTime.of(TOMORROW, START_TIME.plusMinutes(1));

            assertThatThrownBy(() -> slot.block(afterSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already started");
        }
    }

    @Nested
    class Unblock {

        @Test
        void shouldUnblockToAvailableWhenCapacityRemains() {
            slot.book(beforeSlotTime);
            slot.block(beforeSlotTime);

            slot.unblock(beforeSlotTime);

            assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
            assertThat(slot.getBookingCount()).isEqualTo(1);
        }

        @Test
        void shouldUnblockToBookedWhenAtFullCapacity() {
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);
            slot.block(beforeSlotTime);

            slot.unblock(beforeSlotTime);

            assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
            assertThat(slot.getBookingCount()).isEqualTo(MAX_CAPACITY);
        }

        @Test
        void shouldUnblockToAvailableWhenNoBookings() {
            slot.block(beforeSlotTime);

            slot.unblock(beforeSlotTime);

            assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
            assertThat(slot.getBookingCount()).isZero();
        }

        @Test
        void shouldThrowExceptionWhenNotBlocked() {
            assertThatThrownBy(() -> slot.unblock(beforeSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not blocked");
        }

        @Test
        void shouldThrowExceptionWhenSlotTimeHasPassed() {
            slot.block(beforeSlotTime);
            LocalDateTime afterSlotTime = LocalDateTime.of(TOMORROW, START_TIME.plusMinutes(1));

            assertThatThrownBy(() -> slot.unblock(afterSlotTime))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already started");
        }
    }

    @Nested
    class Expire {

        @Test
        void shouldExpireAvailableSlot() {
            slot.expire();

            assertThat(slot.getStatus()).isEqualTo(SlotStatus.EXPIRED);
        }

        @Test
        void shouldExpireBlockedSlot() {
            slot.block(beforeSlotTime);

            slot.expire();

            assertThat(slot.getStatus()).isEqualTo(SlotStatus.EXPIRED);
        }

        @Test
        void shouldNotChangeStatusWhenAlreadyBooked() {
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);

            slot.expire();

            assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        }
    }

    @Nested
    class HasAvailableCapacity {

        @Test
        void shouldReturnTrueWhenAvailableAndHasCapacity() {
            assertThat(slot.hasAvailableCapacity()).isTrue();
        }

        @Test
        void shouldReturnTrueWhenPartiallyBooked() {
            slot.book(beforeSlotTime);

            assertThat(slot.hasAvailableCapacity()).isTrue();
        }

        @Test
        void shouldReturnFalseWhenFullyBooked() {
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);
            slot.book(beforeSlotTime);

            assertThat(slot.hasAvailableCapacity()).isFalse();
        }

        @Test
        void shouldReturnFalseWhenBlocked() {
            slot.block(beforeSlotTime);

            assertThat(slot.hasAvailableCapacity()).isFalse();
        }

        @Test
        void shouldReturnFalseWhenExpired() {
            slot.expire();

            assertThat(slot.hasAvailableCapacity()).isFalse();
        }
    }

    @Nested
    class Equality {

        @Test
        void shouldBeEqualWhenSameId() {
            Slot same = slot;

            assertThat(slot).isEqualTo(same);
        }

        @Test
        void shouldNotBeEqualWhenDifferentId() {
            Slot other = new Slot(TOMORROW, START_TIME, END_TIME, MAX_CAPACITY, BRANCH_ID);

            assertThat(slot).isNotEqualTo(other);
        }

        @Test
        void shouldHaveSameHashCodeForSameSlot() {
            assertThat(slot.hashCode()).isEqualTo(slot.hashCode());
        }
    }

    @Nested
    class Duration {

        @Test
        void shouldCalculateDuration() {
            assertThat(slot.getDuration().toMinutes()).isEqualTo(30);
        }
    }
}
