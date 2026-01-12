package capitec.branch.appointment.slots.infrastructure.holiday;

import capitec.branch.appointment.slots.domain.Holiday;
import capitec.branch.appointment.slots.domain.HolidayClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Set;

@Service
@Slf4j
public class HolidayClientFromNagerClient implements HolidayClient {

    private final RestClient holidaysRestClient;

    public HolidayClientFromNagerClient(@Qualifier("holidaysRestClient") RestClient holidaysRestClient) {
        this.holidaysRestClient = holidaysRestClient;
    }

    @Override
    public Set<Holiday> getHolidays(String countryCode, int year) {

        Set<Holiday> exchange;
        try {

            var uri =String.format("/api/v3/PublicHolidays/%s/%s",year, countryCode);

           exchange = holidaysRestClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange((_, response) -> {

                        if (response.getStatusCode().is2xxSuccessful()) {

                            ObjectMapper mapper = new ObjectMapper();

                            mapper.registerModule(new JavaTimeModule());

                            TypeReference<Set<Holiday>> typeRef = new TypeReference<>() {};

                            return mapper.readValue(response.getBody(), typeRef);
                        } else {
                            log.error("Failed to get HolidayClient from Nager Client, Response status code: {} Response message: {}", response.getStatusCode(), response.getStatusText());
                            return Collections.emptySet();
                        }
                    });

        }catch(Exception e) {

            log.error("Failed to get HolidayClient from Nager Client", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }

        return  exchange;
    }
}
