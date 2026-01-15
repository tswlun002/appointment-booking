package capitec.branch.appointment.kafka.domain;


public interface ErrorEventValue extends EventValue{
     Long getPartition();
     Long getOffset();
     String getException();
     String getExceptionClass();
     String getCauseClass();
     String getStackTrace();
     boolean isRetryable();

}
