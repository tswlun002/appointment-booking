package lunga.appointmentbooking.kafka.domain;

public enum DEAD_LETTER_STATUS {
    RECOVERED,
    DEAD;

    public static boolean isValueOf(String status) {
        for(DEAD_LETTER_STATUS s : DEAD_LETTER_STATUS.values()) {
            if(s.name().equals(status)) {
                return true;
            }
        }
        return false;
    }
}
