package capitec.branch.appointment;

import capitec.branch.appointment.slots.infrastructure.config.SlotRetryableError;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({SlotRetryableError.class})
public class AppointmentBookingApplication {

    static void main(String[] args) {
        SpringApplication.run(AppointmentBookingApplication.class, args);
    }

}
