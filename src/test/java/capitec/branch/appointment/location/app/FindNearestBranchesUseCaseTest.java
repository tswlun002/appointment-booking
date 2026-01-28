package capitec.branch.appointment.location.app;

import capitec.branch.appointment.utils.sharekernel.day.app.GetDateOfNextDaysQuery;
import capitec.branch.appointment.utils.sharekernel.day.domain.Day;
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

@DisplayName("FindNearestBranchesUseCase Integration Test")
class FindNearestBranchesUseCaseTest extends LocationTestBase {

    // Cape Town coordinates
    private static final double CAPE_TOWN_LAT = -33.9249;
    private static final double CAPE_TOWN_LON = 18.4241;
    @Autowired
    private  GetDateOfNextDaysQuery getDateOfNextDaysQuery;

    @Test
    @DisplayName("Should find nearest branches by coordinates")
    void shouldFindNearestBranchesByCoordinates() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                5,
                null
        );

        // When
        List<NearbyBranchDTO> result = findNearestBranchesUseCase.execute(query);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSizeLessThanOrEqualTo(5);

        // Verify branches are actual branches (not ATMs - filtered out)
        assertThat(result)
                .allSatisfy(branch -> {
                    assertThat(branch.branchCode()).isNotNull();
                    assertThat(branch.name()).isNotBlank();
                    assertThat(branch.fullAddress()).isNotBlank();
                    assertThat(branch.fromNearbyLocation()).isFalse();
                });

        // Verify ATMs are filtered out (ATM in response has Code = null)
        assertThat(result)
                .noneMatch(branch -> branch.name().contains("Total Rondebosch Vulstasie"));
    }

    @Test
    @DisplayName("Should find nearest branches with max distance filter")
    void shouldFindNearestBranchesWithMaxDistance() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                10,
                50.0 // 50km max distance
        );

        // When
        List<NearbyBranchDTO> result = findNearestBranchesUseCase.execute(query);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result)
                .allSatisfy(branch -> {
                    assertThat(branch.distanceKm()).isLessThanOrEqualTo(50.0);
                });
    }

    @Test
    @DisplayName("Should return branches sorted by distance")
    void shouldReturnBranchesSortedByDistance() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                10,
                null
        );

        // When
        List<NearbyBranchDTO> result = findNearestBranchesUseCase.execute(query);

        // Then
        assertThat(result).isNotEmpty();

        // Verify sorted by distance (ascending)
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).distanceKm())
                    .isLessThanOrEqualTo(result.get(i + 1).distanceKm());
        }
    }

    @Test
    @DisplayName("Should respect limit parameter")
    void shouldRespectLimitParameter() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                2,
                null
        );

        // When
        List<NearbyBranchDTO> result = findNearestBranchesUseCase.execute(query);

        // Then
        assertThat(result).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should use default limit of 10 when not specified")
    void shouldUseDefaultLimitWhenNotSpecified() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);

        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                null,
                null
        );

        // Then - query should have default limit
        assertThat(query.limit()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should return empty list when no branches found")
    void shouldReturnEmptyListWhenNoBranchesFound() {
        // Given
        stubCapitecApiEmptyResponse(capitecApiWireMock, EMPTY_BRANCH_RESPONSE);

        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                5,
                null
        );

        // When
        List<NearbyBranchDTO> result = findNearestBranchesUseCase.execute(query);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw ResponseStatusException when API is unavailable and no cache exists")
    void shouldThrowExceptionWhenApiUnavailableAndNoCacheExists() {
        // Given
        stubCapitecApiError(capitecApiWireMock);

        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                5,
                null
        );

        // When/Then
        assertThatThrownBy(() -> findNearestBranchesUseCase.execute(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Branch locator service is temporarily unavailable");
    }

    @Test
    @DisplayName("Should return cached data when API is unavailable but cache exists")
    void shouldReturnCachedDataWhenApiUnavailableButCacheExists() {
        // Given - First call to populate cache
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);
        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                5,
                null
        );

        // Populate cache with successful call
        List<NearbyBranchDTO> firstResult = findNearestBranchesUseCase.execute(query);
        assertThat(firstResult).isNotEmpty();
        assertThat(firstResult).allSatisfy(branch -> assertThat(branch.fromNearbyLocation()).isFalse());

        // Now make API return error
        capitecApiWireMock.resetMappings();
        stubCapitecApiError(capitecApiWireMock);

        // When - Second call should use cache fallback
        List<NearbyBranchDTO> fallbackResult = findNearestBranchesUseCase.execute(query);

        // Then - Should return cached data
        assertThat(fallbackResult).isNotEmpty();
        assertThat(fallbackResult.size()).isEqualTo(firstResult.size());
        assertThat(fallbackResult).allSatisfy(branch -> assertThat(branch.fromNearbyLocation()).isFalse());
    }

    @Test
    @DisplayName("Should return nearby cached data when API is unavailable and exact coordinates not cached")
    void shouldReturnNearbyCachedDataWhenExactCoordinatesNotCached() {
        // Given - First call to populate cache with slightly different coordinates
        double cachedLat = CAPE_TOWN_LAT + 0.01; // ~1km difference
        double cachedLon = CAPE_TOWN_LON + 0.01;

        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);
        FindNearestBranchesQuery cachePopulationQuery = new FindNearestBranchesQuery(
                cachedLat,
                cachedLon,
                5,
                null
        );

        // Populate cache with slightly different coordinates
        List<NearbyBranchDTO> cachedResult = findNearestBranchesUseCase.execute(cachePopulationQuery);
        assertThat(cachedResult).isNotEmpty();

        // Now make API return error
        capitecApiWireMock.resetMappings();
        stubCapitecApiError(capitecApiWireMock);

        // When - Query with original coordinates (not in cache, but within 5km radius)
        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                5,
                null
        );

        List<NearbyBranchDTO> fallbackResult = findNearestBranchesUseCase.execute(query);

        // Then - Should return nearby cached data with flag set
        assertThat(fallbackResult).isNotEmpty();
        assertThat(fallbackResult).allSatisfy(branch -> assertThat(branch.fromNearbyLocation()).isTrue());
    }

    @Test
    @DisplayName("Should include branch details in response")
    void shouldIncludeBranchDetailsInResponse() {
        // Given
        stubCapitecApiFailThenSucceed(capitecApiWireMock, CAPITEC_BRANCH_API_RESPONSE);
        FindNearestBranchesQuery query = new FindNearestBranchesQuery(
                CAPE_TOWN_LAT,
                CAPE_TOWN_LON,
                10,
                null
        );

        // When
        List<NearbyBranchDTO> result = findNearestBranchesUseCase.execute(query);

        // Then
        assertThat(result).isNotEmpty();

        NearbyBranchDTO branch = result.stream()
                .filter(b -> b.name().equals("Rondebosch"))
                .findFirst()
                .orElse(null);

        if (branch != null) {
            assertThat(branch.branchCode()).isEqualTo("470010");
            assertThat(branch.fullAddress()).contains("Rondebosch");
            Set<Day> daySet = getDateOfNextDaysQuery.execute(DayOfWeek.MONDAY, DayOfWeek.SUNDAY);

            for (Day day : daySet) {
                Map<LocalDate, OperationTimeDTO> actual = branch.operationTimes();
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
            assertThat(branch.fromNearbyLocation()).isFalse();
        }
    }
}

