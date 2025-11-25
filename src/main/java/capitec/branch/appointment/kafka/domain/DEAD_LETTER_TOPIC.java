package capitec.branch.appointment.kafka.domain;

public enum DEAD_LETTER_TOPIC {
    DEAD("dead-letter"),
    RETRY("retryable-letter");
    private String topic;

    DEAD_LETTER_TOPIC(String s) {
        this.topic=s;
    }
    public String getTopic() {
        return topic;
    }
}
