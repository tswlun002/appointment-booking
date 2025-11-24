package lunga.appointmentbooking.kafka.infrastructure.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "kafka.common-scheduler")
@Component("schedulerProperties")
@Getter
@Setter
public class SchedulerProperties {
    private  String fixedRateMS="60000";
    private  String  fixedDelayMS="30000";
    private  String  initialDelayMS="1000";
    private  String  cron="0 0 0 * * ?";
}
