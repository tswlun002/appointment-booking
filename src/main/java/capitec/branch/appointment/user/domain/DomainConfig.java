package capitec.branch.appointment.user.domain;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class DomainConfig {

    @Bean
    @Primary
    public Validator defaultValidator() {

        return new LocalValidatorFactoryBean();
    }
    @Bean
    public capitec.branch.appointment.utils.Validator validator(){
        return new capitec.branch.appointment.utils.Validator();
    }
}
