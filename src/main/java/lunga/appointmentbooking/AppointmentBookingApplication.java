package lunga.appointmentbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppointmentBookingApplication {

    static void main(String[] args) {
        SpringApplication.run(AppointmentBookingApplication.class, args);
    }

}
