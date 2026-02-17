package capitec.branch.appointment.sharekernel.event.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@JsonTypeName("AppointmentMetadata")
public record AppointmentMetadata(
        @NotNull(message = "AppointmentId is required")
        UUID id,
        @NotBlank(message = "Appointment reference is required")
        String reference,
        @NotBlank(message = "Branch id is required")
        String branchId,
        @NotBlank(message = "Customer username is required")
        String customerUsername,
        @NotNull(message = "Time the event created is required")
        LocalDateTime createdAt,
        Map<String, Object> otherData


) implements MetaData, Serializable {

    public AppointmentMetadata {
        Assert.notNull(id, "Appointment id is required");
        Assert.hasText(reference, "Appointment reference is required");
        Assert.hasText(branchId, "Branch id is required");
        Assert.hasText(customerUsername, "Customer username is required");
        Assert.notNull(createdAt, "Created at is required");

    }

}
