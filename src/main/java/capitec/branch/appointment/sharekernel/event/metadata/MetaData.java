package capitec.branch.appointment.sharekernel.event.metadata;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OTPMetadata.class, name = "OTPMetadata"),
        @JsonSubTypes.Type(value = AppointmentMetadata.class, name = "AppointmentMetadata")
})
public sealed interface MetaData extends Serializable permits AppointmentMetadata, OTPMetadata {
}
