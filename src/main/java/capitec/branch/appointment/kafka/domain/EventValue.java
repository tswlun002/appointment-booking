package capitec.branch.appointment.kafka.domain;

import java.time.LocalDateTime;

public interface EventValue {
      String getKey();
      String getValue();
      String getTraceId();
      String getTopic();
      String getEventId();
      LocalDateTime getPublishTime();
      default String getSchemaVersion() { return "1.0"; }
      default String getSource() { return "unknown"; }
      default String getEventType() { return this.getClass().getSimpleName(); }
}
