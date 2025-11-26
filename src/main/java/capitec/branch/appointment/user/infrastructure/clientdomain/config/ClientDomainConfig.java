package capitec.branch.appointment.user.infrastructure.clientdomain.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientDomainConfig {

    @Value("${client-domain.baseurl}")
    private String baseUrl;


    @Qualifier("clientDomainRestClient")
    @Bean
    public RestClient clientDomainRestClient() {
       return RestClient.builder().baseUrl(baseUrl).build();
    }
}
