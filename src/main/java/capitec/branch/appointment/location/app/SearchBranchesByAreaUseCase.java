package capitec.branch.appointment.location.app;

import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.exeption.BranchLocationServiceException;
import capitec.branch.appointment.location.domain.BranchLocationFetcher;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Use case to search branches by area text input.
 * Searches across city, province, suburb, address, postal code,and branch name.
 */
@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class SearchBranchesByAreaUseCase {

    private final BranchLocationFetcher branchLocationFetcher;

    public List<NearbyBranchDTO> execute(@Valid SearchBranchesByAreaQuery query) {
        log.info("Searching branches by area: {}", query.searchText());

        try {
            List<BranchLocation> branches = searchByArea(query.searchText());

            log.info("Found {} branches matching '{}'", branches.size(), query.searchText());

            return mapToDto(branches);

        } catch (BranchLocationServiceException e) {
            log.error("No cached data available for area: {}", query);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Branch locator service is temporarily unavailable. Please try again later.");
        }
        catch (Exception e) {
            log.warn("Unexpected error while searching branches by area: {}", e.getMessage(),e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",e);
        }
    }

    private List<BranchLocation> searchByArea(String searchText) {
        return branchLocationFetcher.fetchByArea(searchText).stream()
                .filter(BranchLocation::isAvailableForBooking)
                .toList();
    }

    private List<NearbyBranchDTO> mapToDto(List<BranchLocation> branches) {
        return branches.stream()
                .map(branch -> NearbyBranchDTO.from(branch, 0))
                .toList();
    }
}

