package capitec.branch.appointment.sharekernel.schedulers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulingConfig {
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // Even with virtual threads, set a base pool
        scheduler.setThreadNamePrefix("v-thread-scheduling-");
        // This line links it to Virtual Threads
        Thread.ofVirtual().name("vt-sched-", 0).factory();
        return scheduler;
    }
}