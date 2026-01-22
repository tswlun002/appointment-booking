package capitec.branch.appointment.location.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SearchBranchesByAreaUseCase Integration Test")
class SearchBranchesByAreaUseCaseTest extends LocationTestBase {

    @Test
    @DisplayName("Should find branches by city name")
    void shouldFindBranchesByCityName() {
        // Given
        stubCapitecBranchApiForArea("Rondebosch");

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
        stubCapitecBranchApiForArea("Western Cape");

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
        stubCapitecBranchApiForArea("V&A Waterfront");

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
        stubCapitecBranchApiForArea("Cape Town CBD");

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
        stubCapitecBranchApiForArea("Rondebosch");

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Rondebosch");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then - ATM (Total Rondebosch Vulstasie with Code=null) should be filtered out
        assertThat(result)
                .noneMatch(branch -> branch.name().contains("Total Rondebosch Vulstasie"));
    }

    @Test
    @DisplayName("Should return empty list when no branches match")
    void shouldReturnEmptyListWhenNoBranchesMatch() {
        // Given
        stubCapitecBranchApiEmptyResponse();

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
        stubCapitecBranchApiError();

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
        stubCapitecBranchApiForArea("Cape Town");

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("Cape Town");

        // Populate cache with successful call
        List<NearbyBranchDTO> firstResult = searchBranchesByAreaUseCase.execute(query);
        assertThat(firstResult).isNotEmpty();
        assertThat(firstResult).allSatisfy(branch -> assertThat(branch.fromNearbyLocation()).isFalse());

        // Now make API return error
        capitecApiWireMock.resetMappings();
        stubCapitecBranchApiError();

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
        stubCapitecBranchApiForArea("Cape Town");

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
            assertThat(cbdBranch.weekdayHours()).isEqualTo("Monday - Friday, 8am - 5pm");
            assertThat(cbdBranch.handlesHomeLoans()).isTrue();
            assertThat(cbdBranch.businessBankCenter()).isTrue();
            assertThat(cbdBranch.fromNearbyLocation()).isFalse();
        }
    }

    @Test
    @DisplayName("Should return branches with distance as zero and fromNearbyLocation as false for area search")
    void shouldReturnBranchesWithDistanceZeroForAreaSearch() {
        // Given
        stubCapitecBranchApiForArea("Cape Town");

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
        stubCapitecBranchApiForArea("cape town");

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
        stubCapitecBranchApiForArea("V&A Waterfront");

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery("V&A Waterfront");

        // When
        List<NearbyBranchDTO> result = searchBranchesByAreaUseCase.execute(query);

        // Then - V&A Waterfront branch is open on Sundays
        NearbyBranchDTO waterfrontBranch = result.stream()
                .filter(b -> b.name().equals("V&A Waterfront"))
                .findFirst()
                .orElse(null);

        if (waterfrontBranch != null) {
            assertThat(waterfrontBranch.sundayHours()).isEqualTo("Sunday, 10am - 4pm");
        }
    }
}

