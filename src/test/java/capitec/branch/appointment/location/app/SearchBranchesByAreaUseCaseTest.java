package capitec.branch.appointment.location.app;

import capitec.branch.appointment.utils.sharekernel.day.app.GetDateOfNextDaysQuery;
import capitec.branch.appointment.utils.sharekernel.day.domain.Day;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SearchBranchesByAreaUseCase Integration Test")
class SearchBranchesByAreaUseCaseTest extends LocationTestBase {

     @Autowired
     private GetDateOfNextDaysQuery getDateOfNextDaysQuery;

    @Test
    @DisplayName("Should find branches by city name")
    void shouldFindBranchesByCityName() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Rondebosch");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result)
                .allSatisfy(branch -> {
                    assertThat(branch.branchCode()).isNotNull();
                    assertThat(branch.name()).isNotBlank();
                    assertThat(branch.fromNearbyLocation()).isFalse();
                });
    }

    @Test
    @DisplayName("Should find branches by province name")
    void shouldFindBranchesByProvinceName() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);
        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Western Cape");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Should find branches by mall name")
    void shouldFindBranchesByMallName() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("V&A Waterfront");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Should find branches by suburb name")
    void shouldFindBranchesBySuburbName() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Cape Town CBD");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Should filter out ATMs from results")
    void shouldFilterOutATMsFromResults() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Rondebosch");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then - ATM (Total Rondebosch Vulstasie with Code=null) should be filtered out
        assertThat(result)
                .noneMatch(branch -> StringUtils.isBlank(branch.branchCode()));
    }

    @Test
    @DisplayName("Should return empty list when no branches match")
    void shouldReturnEmptyListWhenNoBranchesMatch() {
        // Given
        stubCapitecApiEmptyResponse(capitecApiWireMock, EMPTY_BRANCH_RESPONSE);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("NonExistentArea");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw ResponseStatusException when API is unavailable and no cache exists")
    void shouldThrowExceptionWhenApiUnavailableAndNoCacheExists() {
        // Given
        stubCapitecApiError(capitecApiWireMock);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Cape Town");

        // When/Then
        assertThatThrownBy(() -> searchBranchesByAreaUseCase.execute(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Branch locator service is temporarily unavailable");
    }

    @Test
    @DisplayName("Should return cached data when API is unavailable but cache exists")
    void shouldReturnCachedDataWhenApiUnavailableButCacheExists() {
        // Given - First call to populate cache
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Cape Town");

        // Populate cache with successful call
        List<NearbyBranchDTO> firstResult = searchBranchesByAreaUseCase.execute(query);
        assertThat(firstResult).isNotEmpty();
        assertThat(firstResult).allSatisfy(branch -> assertThat(branch.fromNearbyLocation()).isFalse());

        // Now make API return error
        capitecApiWireMock.resetMappings();
        stubCapitecApiError(capitecApiWireMock);

        // When - Second call should use cache fallback
        List<NearbyBranchDTO> fallbackResult = searchBranchesByAreaUseCase.execute(query);

        // Then - Should return cached data (fromNearbyLocation is always false for area search)
        assertThat(fallbackResult).isNotEmpty();
        assertThat(fallbackResult.size()).isEqualTo(firstResult.size());
        assertThat(fallbackResult).allSatisfy(branch -> assertThat(branch.fromNearbyLocation()).isFalse());
    }

    @Test
    @DisplayName("Should include all branch details in response")
    void shouldIncludeAllBranchDetailsInResponse() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);
        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Cape Town");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then
        assertThat(result).isNotEmpty();

        // Find a specific branch and verify all details
        NearbyBranchDTO cbdBranch = result.stream()
                .filter(b -> b.name().equals("Cape Town CBD"))
                .findFirst()
                .orElse(null);

        if (cbdBranch != null) {
            assertThat(cbdBranch.branchCode()).isEqualTo("470020");
            assertThat(cbdBranch.fullAddress()).contains("Adderley Street");
            assertThat(cbdBranch.latitude()).isEqualTo(-33.925839);
            assertThat(cbdBranch.longitude()).isEqualTo(18.423622);
            Set<Day> daySet = getDateOfNextDaysQuery.execute(DayOfWeek.MONDAY, DayOfWeek.SUNDAY);

            for (Day day : daySet) {
                Map<LocalDate, OperationTimeDTO> actual = cbdBranch.operationTimes();
                if(day.isHoliday()){
                    assertThat(actual.get(day.getDate()))
                            .hasFieldOrPropertyWithValue("closed", true)
                            .hasFieldOrPropertyWithValue("openAt", null)
                            .hasFieldOrPropertyWithValue("closeAt", null);
                }
                if(day.isWeekday()) {
                    assertThat(actual.get(day.getDate()))
                            .hasFieldOrPropertyWithValue("closed", false)
                            .hasFieldOrPropertyWithValue("openAt", LocalTime.of(8, 0))
                            .hasFieldOrPropertyWithValue("closeAt", LocalTime.of(17, 0));
                }
                if(day.getDate().getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
                    assertThat(actual.get(day.getDate()))
                            .hasFieldOrPropertyWithValue("closed", false)
                            .hasFieldOrPropertyWithValue("openAt", LocalTime.of(8, 0))
                            .hasFieldOrPropertyWithValue("closeAt", LocalTime.of(13, 0));
                }
                if(day.getDate().getDayOfWeek().equals(DayOfWeek.SUNDAY)) {

                    assertThat(actual.get(day.getDate()))
                            .hasFieldOrPropertyWithValue("closed", true)
                            .hasFieldOrPropertyWithValue("openAt", null)
                            .hasFieldOrPropertyWithValue("closeAt", null);
                }

            };
            assertThat(cbdBranch.businessBankCenter()).isTrue();
            assertThat(cbdBranch.fromNearbyLocation()).isFalse();
        }
    }

    @Test
    @DisplayName("Should return branches with distance as zero and fromNearbyLocation as false for area search")
    void shouldReturnBranchesWithDistanceZeroForAreaSearch() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Cape Town");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then - Distance should be 0 and fromNearbyLocation false for area search (no reference point)
        assertThat(result)
                .allSatisfy(branch -> {
                    assertThat(branch.distanceKm()).isEqualTo(0.0);
                    assertThat(branch.fromNearbyLocation()).isFalse();
                });
    }

    @Test
    @DisplayName("Should handle case-insensitive search")
    void shouldHandleCaseInsensitiveSearch() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        SearchBranchesByAreaQuery queryLowerCase = new SearchBranchesByAreaQuery("cape town");
        SearchBranchesByAreaQuery queryUpperCase = new SearchBranchesByAreaQuery("CAPE TOWN");

        // When
        List<NearbyBranchDTO> resultLower = searchBranchesByAreaUseCase.execute(queryLowerCase);
        List<NearbyBranchDTO> resultUpper = searchBranchesByAreaUseCase.execute(queryUpperCase);

        // Then - Both should return results (API handles case)
        assertThat(resultLower).isNotEmpty();
        assertThat(resultUpper).isNotEmpty();
    }

    @Test
    @DisplayName("Should include operating hours for branches open on weekends")
    void shouldIncludeOperatingHoursForBranchesOpenOnWeekends() {
        // Given
        stubCapitecApiSuccess(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("V&A Waterfront");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then - V&A Waterfront branch is open on Sundays
        LocalDate now = LocalDate.now();
        for (var i = 0; i < 7; i++) {
            if(now.getDayOfWeek() == DayOfWeek.SATURDAY) {
                break;
            }
            now = now.plusDays(1);
        }
        LocalDate finalNow = now;
        result.stream()
                .filter(b -> b.name().equals("V&A Waterfront"))
                .findFirst().ifPresent(waterfrontBranch ->
                        assertThat(waterfrontBranch.operationTimes().get(finalNow)).hasFieldOrPropertyWithValue("closed",false));

    }
}

