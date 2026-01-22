package capitec.branch.appointment.location.app;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchBranchesByAreaQuery(
        @NotBlank(message = "Search text is required")
        @Size(min = 2, max = 100, message = "Search text must be between 2 and 100 characters")
        String searchText
) {
}

