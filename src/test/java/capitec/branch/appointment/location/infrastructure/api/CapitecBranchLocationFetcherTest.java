package capitec.branch.appointment.location.infrastructure.api;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.exeption.BranchLocationServiceException;
import capitec.branch.appointment.location.domain.Coordinates;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CapitecBranchLocationFetcher Integration Tests")
class CapitecBranchLocationFetcherTest extends AppointmentBookingApplicationTests {

    @Autowired
    private CapitecBranchLocationFetcher branchLocationFetcher;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private CacheManager cacheManager;

    private WireMock capitecApiWireMock;

    private static final double CAPE_TOWN_LAT = -33.9249;
    private static final double CAPE_TOWN_LON = 18.4241;

    private static final String CAPITEC_BRANCH_API_RESPONSE = """
            {
                "Branches": [
                    {
                        "Id": null,
                        "Code": "470010",
                        "Latitude": -33.960553,
                        "Longitude": 18.470156,
                        "Name": "Rondebosch",
                        "AddressLine1": "Shop G21, Cnr Main & Belmont Road, Fountain Centre, Rondebosch, 7700",
                        "AddressLine2": "Fountain Centre",
                        "OpeningHours": "Monday - Friday, 8am - 5pm",
                        "SaturdayHours": "Saturday, 8am - 1pm",
                        "SundayHours": "Closed on Sundays",
                        "PublicHolidayHours": "Closed on Public Holidays",
                        "City": "Rondebosch",
                        "Province": "Western Cape",
                        "IsAtm": false,
                        "CashAccepting": false,
                        "HandlesHomeLoans": false,
                        "IsClosed": false,
                        "BusinessBankCenter": false
                    },
                    {
                        "Id": null,
                        "Code": "470020",
                        "Latitude": -33.925839,
                        "Longitude": 18.423622,
                        "Name": "Cape Town CBD",
                        "AddressLine1": "Shop 5, Cape Town Station Building, Adderley Street",
                        "AddressLine2": null,
                        "OpeningHours": "Monday - Friday, 8am - 5pm",
                        "SaturdayHours": "Saturday, 8am - 1pm",
                        "SundayHours": "Closed on Sundays",
                        "PublicHolidayHours": "Closed on Public Holidays",
                        "City": "Cape Town",
                        "Province": "Western Cape",
                        "IsAtm": false,
                        "CashAccepting": false,
                        "HandlesHomeLoans": true,
                        "IsClosed": false,
                        "BusinessBankCenter": true
                    },
                    {
                        "Id": "SAS29340",
                        "Code": null,
                        "Latitude": -25.7751312,
                        "Longitude": 29.4944725,
                        "Name": "Total Rondebosch ATM",
                        "AddressLine1": "Total Rondebosch Vulstasie, Corner of N11",
                        "AddressLine2": null,
                        "OpeningHours": null,
                        "SaturdayHours": null,
                        "SundayHours": null,
                        "PublicHolidayHours": null,
                        "City": "Middelburg",
                        "Province": "Mpumalanga",
                        "IsAtm": true,
                        "CashAccepting": false,
                        "HandlesHomeLoans": false,
                        "IsClosed": false,
                        "BusinessBankCenter": false
                    }
                ]
            }
            """;

    private static final String EMPTY_BRANCH_RESPONSE = """
            {
                "Branches": []
            }
            """;

    @BeforeEach
    void setup() {
        capitecApiWireMock = new WireMock(
                wiremockClientDomainServer.getHost(),
                wiremockClientDomainServer.getFirstMappedPort()
        );
        capitecApiWireMock.resetMappings();

        // Reset circuit breaker state before each test
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("branchLocatorCircuitBreaker");
        cb.reset();

        // Clear caches
        clearCaches();
    }

    private void clearCaches() {
        Objects.requireNonNull(cacheManager.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_COORDINATES_CACHE))
                .clear();
        Objects.requireNonNull(cacheManager.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_AREA_CACHE))
                .clear();
    }

    @Nested
    @DisplayName("fetchByCoordinates")
    class FetchByCoordinatesTests {

        @Test
        @DisplayName("Should fetch branches by coordinates successfully")
        void shouldFetchBranchesByCoordinatesSuccessfully() {
            // Given
            stubCapitecApiSuccess();
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(2); // ATM is filtered out

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
            stubCapitecApiSuccess();
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
            stubCapitecApiEmptyResponse();
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
            stubCapitecApiSuccess();
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When - First call
            List<BranchLocation> firstResult = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Reset WireMock to verify second call doesn't hit API
            capitecApiWireMock.resetMappings();
            stubCapitecApiError(); // This would fail if called

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
            stubCapitecApiSuccess();
            String searchText = "Cape Town";

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByArea(searchText);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(2); // ATM is filtered out
        }

        @Test
        @DisplayName("Should cache results for same area search (case insensitive)")
        void shouldCacheResultsForSameAreaSearch() {
            // Given
            stubCapitecApiSuccess();

            // When - First call with lowercase
            List<BranchLocation> firstResult = branchLocationFetcher.fetchByArea("cape town");

            // Reset WireMock
            capitecApiWireMock.resetMappings();
            stubCapitecApiError();

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
            stubCapitecApiFailThenSucceed();
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then - Should succeed after retry
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should throw exception after all retries exhausted")
        void shouldThrowExceptionAfterAllRetriesExhausted() {
            // Given - All calls fail
            stubCapitecApiPersistentFailure();
            Coordinates coordinates = new Coordinates(-99.0, -99.0); // Different coordinates to avoid cache

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
            stubCapitecApiPersistentFailure();
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
            stubCapitecApiSuccess();
            Coordinates coordinates = new Coordinates(CAPE_TOWN_LAT, CAPE_TOWN_LON);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then
            BranchLocation capeTownCbd = result.stream()
                    .filter(b -> "470020".equals(b.getBranchCode()))
                    .findFirst()
                    .orElseThrow();

            assertThat(capeTownCbd.getName()).isEqualTo("Cape Town CBD");
            assertThat(capeTownCbd.getCoordinates().latitude()).isEqualTo(-33.925839);
            assertThat(capeTownCbd.getCoordinates().longitude()).isEqualTo(18.423622);
            assertThat(capeTownCbd.getAddress().addressLine1()).isEqualTo("Shop 5, Cape Town Station Building, Adderley Street");
            assertThat(capeTownCbd.getAddress().city()).isEqualTo("Cape Town");
            assertThat(capeTownCbd.getAddress().province()).isEqualTo("Western Cape");
            assertThat(capeTownCbd.getOperatingHours().weekdayHours()).isEqualTo("Monday - Friday, 8am - 5pm");
            assertThat(capeTownCbd.getOperatingHours().saturdayHours()).isEqualTo("Saturday, 8am - 1pm");
            assertThat(capeTownCbd.isBusinessBankCenter()).isTrue();
            assertThat(capeTownCbd.isClosed()).isFalse();
        }

        @Test
        @DisplayName("Should handle null optional fields gracefully")
        void shouldHandleNullOptionalFieldsGracefully() {
            // Given
            String responseWithNulls = """
                    {
                        "Branches": [
                            {
                                "Id": null,
                                "Code": "470099",
                                "Latitude": -33.960553,
                                "Longitude": 18.470156,
                                "Name": null,
                                "AddressLine1": null,
                                "AddressLine2": null,
                                "OpeningHours": null,
                                "SaturdayHours": null,
                                "SundayHours": null,
                                "PublicHolidayHours": null,
                                "City": null,
                                "Province": null,
                                "IsAtm": false,
                                "CashAccepting": false,
                                "HandlesHomeLoans": null,
                                "IsClosed": null,
                                "BusinessBankCenter": null
                            }
                        ]
                    }
                    """;
            stubCapitecApiWithResponse(responseWithNulls);
            Coordinates coordinates = new Coordinates(-35.0, 19.0);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then
            assertThat(result).hasSize(1);
            BranchLocation branch = result.get(0);
            assertThat(branch.getName()).isEqualTo("Capitec Branch"); // Default
            assertThat(branch.getAddress().addressLine1()).isEqualTo("Unknown");
            assertThat(branch.getAddress().city()).isEqualTo("Unknown");
            assertThat(branch.getAddress().province()).isEqualTo("Unknown");
            assertThat(branch.getOperatingHours().weekdayHours()).isEqualTo("Monday - Friday, 8am - 5pm"); // Default
            assertThat(branch.isBusinessBankCenter()).isFalse();
            assertThat(branch.isClosed()).isFalse();
        }

        @Test
        @DisplayName("Should skip branches with missing coordinates")
        void shouldSkipBranchesWithMissingCoordinates() {
            // Given
            String responseWithMissingCoords = """
                    {
                        "Branches": [
                            {
                                "Code": "470001",
                                "Latitude": null,
                                "Longitude": 18.470156,
                                "Name": "Branch Without Lat",
                                "IsAtm": false
                            },
                            {
                                "Code": "470002",
                                "Latitude": -33.960553,
                                "Longitude": null,
                                "Name": "Branch Without Lon",
                                "IsAtm": false
                            },
                            {
                                "Code": "470003",
                                "Latitude": -33.960553,
                                "Longitude": 18.470156,
                                "Name": "Valid Branch",
                                "IsAtm": false
                            }
                        ]
                    }
                    """;
            stubCapitecApiWithResponse(responseWithMissingCoords);
            Coordinates coordinates = new Coordinates(-36.0, 20.0);

            // When
            List<BranchLocation> result = branchLocationFetcher.fetchByCoordinates(coordinates);

            // Then - Only the valid branch should be returned
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBranchCode()).isEqualTo("470003");
            assertThat(result.get(0).getName()).isEqualTo("Valid Branch");
        }
    }

    // ==================== Helper Methods ====================

    private void stubCapitecApiSuccess() {
        capitecApiWireMock.register(
                post(urlPathEqualTo("/Branch"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(CAPITEC_BRANCH_API_RESPONSE))
        );
    }

    private void stubCapitecApiWithResponse(String response) {
        capitecApiWireMock.register(
                post(urlPathEqualTo("/Branch"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(response))
        );
    }

    private void stubCapitecApiEmptyResponse() {
        capitecApiWireMock.register(
                post(urlPathEqualTo("/Branch"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(EMPTY_BRANCH_RESPONSE))
        );
    }

    private void stubCapitecApiError() {
        capitecApiWireMock.register(
                post(urlPathEqualTo("/Branch"))
                        .willReturn(aResponse()
                                .withStatus(500)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"error\": \"Internal Server Error\"}"))
        );
    }

    private void stubCapitecApiPersistentFailure() {
        capitecApiWireMock.register(
                post(urlPathEqualTo("/Branch"))
                        .willReturn(aResponse()
                                .withStatus(503)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"error\": \"Service Unavailable\"}"))
        );
    }

    private void stubCapitecApiFailThenSucceed() {
        // First call fails
        capitecApiWireMock.register(
                post(urlPathEqualTo("/Branch"))
                        .inScenario("Retry Scenario")
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(aResponse()
                                .withStatus(503)
                                .withBody("{\"error\": \"Service Unavailable\"}"))
                        .willSetStateTo("RETRY_1")
        );

        // Second call succeeds
        capitecApiWireMock.register(
                post(urlPathEqualTo("/Branch"))
                        .inScenario("Retry Scenario")
                        .whenScenarioStateIs("RETRY_1")
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(CAPITEC_BRANCH_API_RESPONSE))
        );
    }
}

