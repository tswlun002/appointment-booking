package capitec.branch.appointment.location.infrastructure.api;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.sharekernel.day.app.GetDateOfNextDaysQuery;
import capitec.branch.appointment.sharekernel.day.domain.Day;
import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.exeption.BranchLocationServiceException;
import capitec.branch.appointment.location.domain.Coordinates;
import capitec.branch.appointment.location.domain.OperationTime;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CapitecBranchLocationFetcher Integration Tests")
class CapitecBranchLocationFetcherTest extends AppointmentBookingApplicationTests {

    @Autowired
    private CapitecBranchLocationFetcher branchLocationFetcher;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private GetDateOfNextDaysQuery getDateOfNextDaysQuery;

    @Autowired
    @Qualifier("branchLocationCacheManager")
    private CacheManager cacheManager;

    private WireMock capitecApiWireMock;

    private static final double CAPE_TOWN_LAT = -33.9249;
    private static final double CAPE_TOWN_LON = 18.4241;

    private static final String CAPITEC_BRANCH_API_RESPONSE = capitecApiBranchResponse();

    private static final String EMPTY_BRANCH_RESPONSE = capitecApiBranchEmptyResponse();

    @BeforeEach
    void setup() {
        capitecApiWireMock = new WireMock(
                wiremockContainer.getHost(),
                wiremockContainer.getFirstMappedPort()
        );
        capitecApiWireMock.resetMappings();

        // Reset circuit breaker state before each test
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("branchLocatorCircuitBreaker");
        cb.reset();

        // Clear caches
        clearCaches();
    }

    private void clearCaches() {
        var cache = new Cache[]{
                cacheManager.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_COORDINATES_CACHE),
                cacheManager.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_AREA_CACHE)
        };
        for (Cache cache1 : cache) {
            if(cache1!=null) {
                cache1.clear();
            }
        }
    }

    @Nested
    @DisplayName("fetchByCoordinates")
    class FetchByCoordinatesTests {

        @Test
        @DisplayName("Should fetch branches by coordinates successfully")
        void shouldFetchBranchesByCoordinatesSuccessfully() {
            // Given
            stubCapitecApiSuccess(capitecApiWireMock,CAPITEC_BRANCH_API_RESPONSE);
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(7); // ATM is filtered out

            // Verify first branch
            BranchLocation rondebosch = result.stream()
                    .filter(b -> "470010".equals(b.getBranchCode()))
                    .findFirst()
                    .orElseThrow();
            assertThat(rondebosch.getName()).isEqualTo("Rondebosch");
            assertThat(rondebosch.getAddress().city()).isEqualTo("Rondebosch");
            assertThat(rondebosch.getAddress().province()).isEqualTo("Western Cape");
        }

        @Test
        @DisplayName("Should filter out ATMs from response")
        void shouldFilterOutAtmsFromResponse() {
            // Given
            stubCapitecApiSuccess(capitecApiWireMock,CAPITEC_BRANCH_API_RESPONSE);
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then
            assertThat(result)
                    .noneMatch(branch -> branch.getName().contains("ATM"))
                    .noneMatch(branch -> branch.getBranchCode() == null);
        }

        @Test
        @DisplayName("Should return empty list when no branches found")
        void shouldReturnEmptyListWhenNoBranchesFound() {
            // Given
            stubCapitecApiEmptyResponse(capitecApiWireMock,EMPTY_BRANCH_RESPONSE);
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should cache results for same coordinates")
        void shouldCacheResultsForSameCoordinates() {
            // Given
            stubCapitecApiSuccess(capitecApiWireMock,CAPITEC_BRANCH_API_RESPONSE);
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When - First call
            List<BranchLocation> firstResult = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Reset WireMock to verify second call doesn't hit API
            capitecApiWireMock.resetMappings();
            stubCapitecApiError(capitecApiWireMock); // This would fail if called

            // When - Second call (should use cache)
            List<BranchLocation> secondResult = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then
            assertThat(firstResult).isEqualTo(secondResult);
        }
    }

    @Nested
    @DisplayName("fetchByArea")
    class FetchByAreaTests {

        @Test
        @DisplayName("Should fetch branches by area successfully")
        void shouldFetchBranchesByAreaSuccessfully() {
            // Given
            stubCapitecApiSuccess(capitecApiWireMock,CAPITEC_BRANCH_API_RESPONSE);
            String searchText = "Cape Town";

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByArea(searchText);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(7); // ATM is filtered out
        }

        @Test
        @DisplayName("Should cache results for same area search (case insensitive)")
        void shouldCacheResultsForSameAreaSearch() {
            // Given
            stubCapitecApiSuccess(capitecApiWireMock,CAPITEC_BRANCH_API_RESPONSE);

            // When - First call with lowercase
            List<BranchLocation> firstResult = branchLocationFetcher.fetchByArea("cape town");

            // Reset WireMock
            capitecApiWireMock.resetMappings();
            stubCapitecApiError(capitecApiWireMock);

            // When - Second call with uppercase (should use cache due to toLowerCase in key)
            List<BranchLocation> secondResult = branchLocationFetcher.fetchByArea("CAPE TOWN");

            // Then
            assertThat(firstResult).isEqualTo(secondResult);
        }
    }

    @Nested
    @DisplayName("Resilience - Retry")
    class RetryTests {

        @Test
        @DisplayName("Should retry on transient failure and succeed")
        void shouldRetryOnTransientFailureAndSucceed() {
            // Given - First call fails, second succeeds
            stubCapitecApiFailThenSucceed(capitecApiWireMock,CAPITEC_BRANCH_API_RESPONSE);
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then - Should succeed after retry
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(7);
        }

        @Test
        @DisplayName("Should throw exception after all retries exhausted")
        void shouldThrowExceptionAfterAllRetriesExhausted() {
            // Given - All calls fail
            stubCapitecApiPersistentFailure(capitecApiWireMock);
            Coordinates coordinates = new Coordinates(-90.0, -90.0); // Different coordinates to avoid cache

            // When/Then
            assertThatThrownBy(() -> branchLocationFetcher.fetchByCoordinates(coordinates))
                    .isInstanceOf(BranchLocationServiceException.class)
                    .hasMessageContaining("Unable to fetch branches");
        }
    }

    @Nested
    @DisplayName("Resilience - Circuit Breaker")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Should open circuit breaker after failures threshold")
        void shouldOpenCircuitBreakerAfterFailuresThreshold() {
            // Given
            stubCapitecApiPersistentFailure(capitecApiWireMock);
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("branchLocatorCircuitBreaker");

            // When - Make enough calls to trigger circuit breaker (minimum 5 calls, 65% failure rate)
            for (int i = 0; i < 6; i++) {
                try {
                    Coordinates coordinates = new Coordinates(-90.0 + i, -90.0 + i); // Different coords
                    branchLocationFetcher.fetchByCoordinates(coordinates);
                } catch (BranchLocationServiceException ignored) {
                    // Expected
                }
            }

            // Then
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("Should fail fast when circuit breaker is open")
        void shouldFailFastWhenCircuitBreakerIsOpen() {
            // Given - Force circuit breaker to open
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("branchLocatorCircuitBreaker");
            cb.transitionToOpenState();

            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);
            clearCaches(); // Ensure we don't get cached result

            // When/Then
            assertThatThrownBy(() -> branchLocationFetcher.fetchByCoordinates(coordinates))
                    .isInstanceOf(BranchLocationServiceException.class)
                    .hasMessageContaining("temporarily unavailable");

        }
    }

    @Nested
    @DisplayName("Response Mapping")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should map branch with all fields correctly")
        void shouldMapBranchWithAllFieldsCorrectly() {
            // Given
            stubCapitecApiSuccess(capitecApiWireMock,CAPITEC_BRANCH_API_RESPONSE);
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then
            BranchLocation capeTownCbd = result.stream()
                    .filter(b -> "SAS29300".equals(b.getBranchId()))
                    .findFirst()
                    .orElseThrow();

            assertThat(capeTownCbd.getName()).isEqualTo("Cape Town CBD");
            assertThat(capeTownCbd.getCoordinates().latitude()).isEqualTo(-33.925839);
            assertThat(capeTownCbd.getCoordinates().longitude()).isEqualTo(18.423622);
            assertThat(capeTownCbd.getAddress().addressLine1()).isEqualTo("Shop 5, Cape Town Station Building, Adderley Street");
            assertThat(capeTownCbd.getAddress().city()).isEqualTo("Cape Town");
            assertThat(capeTownCbd.getAddress().province()).isEqualTo("Western Cape");
            assertThat(capeTownCbd.isBusinessBankCenter()).isTrue();
            assertThat(capeTownCbd.isClosed()).isFalse();

            Set<Day> daySet = getDateOfNextDaysQuery.execute(LocalDate.now(), LocalDate.now().plusDays(6));

            for (Day day : daySet) {
                Map<LocalDate, OperationTime> actual = capeTownCbd.getDailyOperationTimes();
                if(day.isHoliday()){
                    assertThat(actual.get(day.getDate()))
                            .hasFieldOrPropertyWithValue("closed", true)
                            .hasFieldOrPropertyWithValue("openAt", null)
                            .hasFieldOrPropertyWithValue("closeAt", null)
                            .hasFieldOrPropertyWithValue("isHoliday", true);
                }
                if(day.isWeekday()) {
                    assertThat(actual.get(day.getDate()))
                            .hasFieldOrPropertyWithValue("closed", false)
                            .hasFieldOrPropertyWithValue("openAt", LocalTime.of(8, 0))
                            .hasFieldOrPropertyWithValue("closeAt", LocalTime.of(17, 0))
                            .hasFieldOrPropertyWithValue("isHoliday", false);
                }
                if(day.getDate().getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
                    assertThat(actual.get(day.getDate()))
                            .hasFieldOrPropertyWithValue("closed", false)
                            .hasFieldOrPropertyWithValue("openAt", LocalTime.of(8, 0))
                            .hasFieldOrPropertyWithValue("closeAt", LocalTime.of(13, 0))
                            .hasFieldOrPropertyWithValue("isHoliday", false);
                }
                if(day.getDate().getDayOfWeek().equals(DayOfWeek.SUNDAY)) {

                    assertThat(actual.get(day.getDate()))
                            .hasFieldOrPropertyWithValue("closed", true)
                            .hasFieldOrPropertyWithValue("openAt", null)
                            .hasFieldOrPropertyWithValue("closeAt", null)
                            .hasFieldOrPropertyWithValue("isHoliday", false);
                }

            }
        }
    }


}

