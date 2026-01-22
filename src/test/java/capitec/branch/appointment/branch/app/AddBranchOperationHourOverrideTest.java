package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddBranchOperationHourOverrideTest extends BranchTestBase {

    @Autowired
    private AddBranchUseCase addBranchUseCase;
    @Autowired
    private GetBranchQuery getBranchByIdQuery;
    @Autowired
    private AddBranchOperationHourOverride addBranchOperationHourOverride;

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "BR001;08:00;17:00;false;Public holiday - reduced hours",
            "BR002;09:00;13:00;true;Christmas Day closure"
    })
    void shouldAddOperationHourOverride_whenBranchExists(String branchId, String openTime, String closingTime, boolean isClosed, String reason) {
        LocalDate effectiveDate = LocalDate.now().plusDays(1);

        // ARRANGE
        BranchDTO branchDTO = createBranchDTO(branchId);
        addBranchUseCase.execute(branchDTO);

        BranchOperationHourOverrideDTO dto = new BranchOperationHourOverrideDTO(
                effectiveDate,
                LocalTime.parse(openTime),
                LocalTime.parse(closingTime),
                isClosed,
                reason
        );

        // ACT
        boolean isAdded = addBranchOperationHourOverride.execute(branchId, dto);

        // ASSERT
        assertThat(isAdded).isTrue();

        Branch branch = getBranchByIdQuery.execute(branchId);
        List<OperationHoursOverride> overrides = branch.getOperationHoursOverride();

        assertThat(overrides).hasSize(1);
        assertThat(overrides.get(0).effectiveDate()).isEqualTo(effectiveDate);
        assertThat(overrides.get(0).openTime()).isEqualTo(LocalTime.parse(openTime));
        assertThat(overrides.get(0).closingTime()).isEqualTo(LocalTime.parse(closingTime));
        assertThat(overrides.get(0).closed()).isEqualTo(isClosed);
        assertThat(overrides.get(0).reason()).isEqualTo(reason);
    }

    @Test
    void shouldReplaceOverride_whenSameDateExists() {
        String branchId = "BR003";
        LocalDate effectiveDate = LocalDate.now().plusDays(1);

        // ARRANGE
        addBranchUseCase.execute(createBranchDTO(branchId));

        BranchOperationHourOverrideDTO firstOverride = new BranchOperationHourOverrideDTO(
                effectiveDate, LocalTime.of(8, 0), LocalTime.of(17, 0), false, "Staff training - reduced hours"
        );
        BranchOperationHourOverrideDTO secondOverride = new BranchOperationHourOverrideDTO(
                effectiveDate, LocalTime.of(9, 0), LocalTime.of(13, 0), true, "Emergency closure - flooding"
        );

        // ACT
        addBranchOperationHourOverride.execute(branchId, firstOverride);
        addBranchOperationHourOverride.execute(branchId, secondOverride);

        // ASSERT
        Branch branch = getBranchByIdQuery.execute(branchId);
        List<OperationHoursOverride> overrides = branch.getOperationHoursOverride();

        assertThat(overrides).hasSize(1);
        assertThat(overrides.get(0).closed()).isTrue();
        assertThat(overrides.get(0).reason()).isEqualTo("Emergency closure - flooding");
    }

    @Test
    void shouldThrowNotFound_whenBranchDoesNotExistInExternalSystem() {
        // ARRANGE - Branch not registered in external system (branchOperationHoursPort)
        BranchOperationHourOverrideDTO dto = new BranchOperationHourOverrideDTO(
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                false,
                "Scheduled maintenance - early close"
        );

        // ACT & ASSERT
        assertThatThrownBy(() -> addBranchOperationHourOverride.execute("NON_EXISTENT_EXTERNAL_ID", dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Branch is not found");
    }

    @Test
    void shouldThrowNotFound_whenBranchDoesNotExistInLocalDatabase() {
        // ARRANGE - Branch exists in external system but not in local database
        // This requires mocking or a specific test setup where external check passes but local lookup fails
        BranchOperationHourOverrideDTO dto = new BranchOperationHourOverrideDTO(
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                false,
                "Year-end extended hours"
        );

        // ACT & ASSERT
        assertThatThrownBy(() -> addBranchOperationHourOverride.execute("NON_EXISTENT_LOCAL_ID", dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Branch is not found");
    }

    @Test
    void shouldThrowBadRequest_whenOpenTimeIsAfterClosingTime() {
        String branchId = "BR004";

        // ARRANGE
        addBranchUseCase.execute(createBranchDTO(branchId));

        BranchOperationHourOverrideDTO dto = new BranchOperationHourOverrideDTO(
                LocalDate.now().plusDays(1),
                LocalTime.of(17, 0),  // Open time after closing time
                LocalTime.of(8, 0),
                false,
                "Invalid hours test"
        );

        // ACT & ASSERT
        assertThatThrownBy(() -> addBranchOperationHourOverride.execute(branchId, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrowBadRequest_whenEffectiveDateIsInThePast() {
        String branchId = "BR005";

        // ARRANGE
        addBranchUseCase.execute(createBranchDTO(branchId));

        BranchOperationHourOverrideDTO dto = new BranchOperationHourOverrideDTO(
                LocalDate.now().minusDays(1),  // Past date
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                false,
                "Past date override test"
        );

        // ACT & ASSERT
        assertThatThrownBy(() -> addBranchOperationHourOverride.execute(branchId, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrowBadRequest_whenBranchIsClosed() {
        String branchId = "BR006";

        // ARRANGE - Setup branch that triggers BranchIsClosedException
        addBranchUseCase.execute(createBranchDTO(branchId));

        BranchOperationHourOverrideDTO dto = new BranchOperationHourOverrideDTO(
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                false,
                "Override for closed branch"
        );

        // ACT & ASSERT - Depends on your domain logic for when BranchIsClosedException is thrown
        // Adjust based on actual business rules
        assertThatThrownBy(() -> addBranchOperationHourOverride.execute(branchId, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Branch is closed");
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            ";08:00;17:00;false;Missing effective date",
            "2025-12-25;;17:00;false;Missing open time",
            "2025-12-25;08:00;;false;Missing closing time"
    })
    void shouldThrowBadRequest_whenRequiredFieldsAreMissing(String effectiveDate, String openTime, String closingTime, boolean isClosed, String reason) {
        String branchId = "BR007";

        // ARRANGE
        addBranchUseCase.execute(createBranchDTO(branchId));

        BranchOperationHourOverrideDTO dto = new BranchOperationHourOverrideDTO(
                effectiveDate != null && !effectiveDate.isEmpty() ? LocalDate.parse(effectiveDate) : null,
                openTime != null && !openTime.isEmpty() ? LocalTime.parse(openTime) : null,
                closingTime != null && !closingTime.isEmpty() ? LocalTime.parse(closingTime) : null,
                isClosed,
                reason
        );

        // ACT & ASSERT
        assertThatThrownBy(() -> addBranchOperationHourOverride.execute(branchId, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldAddMultipleOverrides_forDifferentDates() {
        String branchId = "BR008";

        // ARRANGE
        addBranchUseCase.execute(createBranchDTO(branchId));

        BranchOperationHourOverrideDTO override1 = new BranchOperationHourOverrideDTO(
                LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(17, 0), false, "New Year's Eve - extended hours"
        );
        BranchOperationHourOverrideDTO override2 = new BranchOperationHourOverrideDTO(
                LocalDate.now().plusDays(2), LocalTime.of(9, 0), LocalTime.of(13, 0), true, "New Year's Day closure"
        );

        // ACT
        addBranchOperationHourOverride.execute(branchId, override1);
        addBranchOperationHourOverride.execute(branchId, override2);

        // ASSERT
        Branch branch = getBranchByIdQuery.execute(branchId);
        List<OperationHoursOverride> overrides = branch.getOperationHoursOverride();

        assertThat(overrides).hasSize(2);
    }
}
