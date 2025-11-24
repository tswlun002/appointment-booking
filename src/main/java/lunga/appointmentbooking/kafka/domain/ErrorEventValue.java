package lunga.appointmentbooking.kafka.domain;


public interface ErrorEventValue extends EventValue{
     Long getPartition();
     Long getOffset();
     String getException();
     String getExceptionClass();
     String getCauseClass();
     String getStackTrace();
     boolean isRetryable();
     int getRetryCount();
     void setRetryCount(int count);
     void setDeadLetterStatus(DEAD_LETTER_STATUS status);
     DEAD_LETTER_STATUS getDeadLetterStatus();
     void setPartition(Long partition);
     void setOffset(Long offset);
}
