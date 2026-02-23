package capitec.branch.appointment.slots.infrastructure.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.Set;

public record GenerateSlotRequestBody(
         Set<@NotBlank(message = "Branch id are required") String> branches,
         @NotNull(message = "Date is required")
         LocalDate fromDate,
         @Min(value = 1, message = "Number of rollingWindowDays must be a day or more")
         @NotNull(message = "Rolling number of days is required")
         Integer rollingWindowDays
) {
    public GenerateSlotRequestBody {
        Assert.notNull(fromDate, "fromDate is required");
        Assert.isTrue(!fromDate.isBefore(LocalDate.now()), "fromDate must be now or future date");
        Assert.notNull(rollingWindowDays, "rollingWindowDays is required");
        Assert.isTrue(rollingWindowDays >= 1, "Number of rollingWindowDays must be a day or more");
    }
}
