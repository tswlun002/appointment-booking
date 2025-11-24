package lunga.appointmentbooking.kafka.domain;

import java.time.LocalDateTime;

public interface EventValue {
      String getKey();
      String getValue();
      String getTraceId();
      String getTopic();
      String getEventId();
      LocalDateTime getPublishTime();
}
