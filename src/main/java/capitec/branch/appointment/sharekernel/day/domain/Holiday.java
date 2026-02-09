package capitec.branch.appointment.sharekernel.day.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Holiday(
        LocalDate date,
        String localName,
        String name,
        String countryCode,
        Boolean fixed,
        Boolean global
) {
}
