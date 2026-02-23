package capitec.branch.appointment.branch.domain;

import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchTest {

    private static final String BRANCH_ID = "BR001";
    private static final String BRANCH_NAME = "Capitec Head office";


    private static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(17, 0);

    @Nested
    class ConstructorTests {

        @Test
        void shouldCreateBranch_whenValidBranchId() {
            Branch branch = new Branch(BRANCH_ID,BRANCH_NAME);

            assertThat(branch.getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(branch.getBranchAppointmentInfo()).isEmpty();
            assertThat(branch.getOperationHoursOverride()).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void shouldThrowException_whenBranchIdIsBlank(String branchId) {
            assertThatThrownBy(() -> new Branch(branchId,BRANCH_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch ID cannot be blank");
        }
    }

    @Nested
    class UpdateAppointmentInfoTests {

        private Branch branch;

        @BeforeEach
        void setUp() {
            branch = new Branch(BRANCH_ID,BRANCH_NAME);
        }

        @Test
        void shouldAddAppointmentInfo_whenListIsEmpty() {
            LocalDate day = LocalDate.now().plusDays(1);
            BranchAppointmentInfo info = createAppointmentInfo(day, Duration.ofMinutes(30));

            branch.updateAppointmentInfo(DayType.valueOf(day.getDayOfWeek().name()), info, OPEN_TIME, CLOSE_TIME);

            assertThat(branch.getBranchAppointmentInfo()).hasSize(1);
            assertThat(branch.getBranchAppointmentInfo().get(0)).isEqualTo(info);
        }

        @Test
        void shouldReplaceAppointmentInfo_whenSameDayExists() {
            LocalDate day = LocalDate.now().plusDays(1);
            BranchAppointmentInfo oldInfo = createAppointmentInfo(day, Duration.ofMinutes(30));
            BranchAppointmentInfo newInfo = createAppointmentInfo(day, Duration.ofMinutes(45));

            branch.updateAppointmentInfo(DayType.valueOf(day.getDayOfWeek().name()), oldInfo, OPEN_TIME, CLOSE_TIME);
            branch.updateAppointmentInfo(DayType.valueOf(day.getDayOfWeek().name()), newInfo, OPEN_TIME, CLOSE_TIME);

            assertThat(branch.getBranchAppointmentInfo()).hasSize(1);
            assertThat(branch.getBranchAppointmentInfo().get(0).slotDuration()).isEqualTo(Duration.ofMinutes(45));
        }

        @Test
        void shouldThrowException_whenSlotDurationExceedsOperatingHours() {
            LocalDate day = LocalDate.now().plusDays(1);
            Duration longDuration = Duration.ofHours(10);
            BranchAppointmentInfo info = createAppointmentInfo(day, longDuration);

            assertThatThrownBy(() -> branch.updateAppointmentInfo(DayType.valueOf(day.getDayOfWeek().name()), info, OPEN_TIME, CLOSE_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Appointment slots must be within branch operating hours");
        }

        @Test
        void shouldThrowException_whenOpenTimeIsNull() {
            LocalDate day = LocalDate.now().plusDays(1);
            BranchAppointmentInfo info = createAppointmentInfo(day, Duration.ofMinutes(30));

            assertThatThrownBy(() -> branch.updateAppointmentInfo(DayType.valueOf(day.getDayOfWeek().name()), info, null, CLOSE_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch open time cannot be null");
        }

        @Test
        void shouldThrowException_whenClosingTimeIsNull() {
            LocalDate day = LocalDate.now().plusDays(1);
            BranchAppointmentInfo info = createAppointmentInfo(day, Duration.ofMinutes(30));

            assertThatThrownBy(() -> branch.updateAppointmentInfo(DayType.valueOf(day.getDayOfWeek().name()), info, OPEN_TIME, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch closing time cannot be null");
        }
    }

    @Nested
    class UpdateOperationHoursOverrideTests {

        private Branch branch;

        @BeforeEach
        void setUp() {
            branch = new Branch(BRANCH_ID,BRANCH_NAME);
        }

        @Test
        void shouldAddOverride_whenListIsEmpty() {
            LocalDate effectiveDate = LocalDate.now().plusDays(1);
            OperationHoursOverride override = createOverride(effectiveDate, false, "Public holiday - reduced hours");

            branch.updateOperationHoursOverride(override);

            assertThat(branch.getOperationHoursOverride()).hasSize(1);
            assertThat(branch.getOperationHoursOverride().get(0)).isEqualTo(override);
        }

        @Test
        void shouldReplaceOverride_whenSameDateExists() {
            LocalDate effectiveDate = LocalDate.now().plusDays(1);
            OperationHoursOverride oldOverride = createOverride(effectiveDate, false, "Staff training - reduced hours");
            OperationHoursOverride newOverride = createOverride(effectiveDate, true, "Emergency closure - flooding");

            branch.updateOperationHoursOverride(oldOverride);
            branch.updateOperationHoursOverride(newOverride);

            assertThat(branch.getOperationHoursOverride()).hasSize(1);
            assertThat(branch.getOperationHoursOverride().get(0).closed()).isTrue();
        }

        @Test
        void shouldAddMultipleOverrides_forDifferentDates() {
            OperationHoursOverride override1 = createOverride(LocalDate.now().plusDays(1), false, "Year-end extended hours");
            OperationHoursOverride override2 = createOverride(LocalDate.now().plusDays(2), true, "New Year's Day closure");

            branch.updateOperationHoursOverride(override1);
            branch.updateOperationHoursOverride(override2);

            assertThat(branch.getOperationHoursOverride()).hasSize(2);
        }
    }

    @Nested
    class SetBranchAppointmentInfoTests {

        private Branch branch;

        @BeforeEach
        void setUp() {
            branch = new Branch(BRANCH_ID,BRANCH_NAME);
        }

        @Test
        void shouldSetAppointmentInfoList() {
            List<BranchAppointmentInfo> infoList = List.of(
                    createAppointmentInfo(LocalDate.now().plusDays(1), Duration.ofMinutes(30)),
                    createAppointmentInfo(LocalDate.now().plusDays(2), Duration.ofMinutes(45))
            );

            branch.setBranchAppointmentInfo(infoList, OPEN_TIME, CLOSE_TIME);

            assertThat(branch.getBranchAppointmentInfo()).hasSize(2);
        }

        @Test
        void shouldThrowException_whenAnyInfoExceedsOperatingHours() {
            List<BranchAppointmentInfo> infoList = List.of(
                    createAppointmentInfo(LocalDate.now().plusDays(1), Duration.ofMinutes(30)),
                    createAppointmentInfo(LocalDate.now().plusDays(2), Duration.ofHours(10))
            );

            assertThatThrownBy(() -> branch.setBranchAppointmentInfo(infoList, OPEN_TIME, CLOSE_TIME))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class SetOperationHoursOverrideTests {

        @Test
        void shouldSetOverrideList() {
            Branch branch = new Branch(BRANCH_ID,BRANCH_NAME);
            List<OperationHoursOverride> overrides = List.of(
                    createOverride(LocalDate.now().plusDays(1), false, "Scheduled maintenance - early close"),
                    createOverride(LocalDate.now().plusDays(2), true, "Christmas Day closure")
            );

            branch.setOperationHoursOverride(overrides);

            assertThat(branch.getOperationHoursOverride()).hasSize(2);
        }
    }

    @Nested
    class EqualsAndHashCodeTests {

        @Test
        void shouldBeEqual_whenSameBranchId() {
            Branch branch1 = new Branch(BRANCH_ID,BRANCH_NAME);
            Branch branch2 = new Branch(BRANCH_ID,BRANCH_NAME);

            assertThat(branch1).isEqualTo(branch2);
            assertThat(branch1.hashCode()).isEqualTo(branch2.hashCode());
        }

        @Test
        void shouldNotBeEqual_whenDifferentBranchId() {
            Branch branch1 = new Branch("BR001",BRANCH_NAME);
            Branch branch2 = new Branch("BR002",BRANCH_NAME);

            assertThat(branch1).isNotEqualTo(branch2);
        }
    }

    // Helper methods
    private BranchAppointmentInfo createAppointmentInfo(LocalDate day, Duration slotDuration) {
        return new BranchAppointmentInfo(slotDuration, 0.8, 5, DayType.valueOf(day.getDayOfWeek().name()),2);
    }

    private OperationHoursOverride createOverride(LocalDate effectiveDate, boolean closed, String reason) {
        return new OperationHoursOverride(
                effectiveDate,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                closed,
                reason
        );
    }
}
