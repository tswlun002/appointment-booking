package capitec.branch.appointment.day.infrastructure.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HolidayClientConfig {

    @Value("${holidays-client.baseurl}")
    private String baseUrl;


    @Qualifier("holidaysRestClient")
    @Bean
    public RestClient holidaysRestClient() {
       return RestClient.builder().baseUrl(baseUrl).build();
    }
}
