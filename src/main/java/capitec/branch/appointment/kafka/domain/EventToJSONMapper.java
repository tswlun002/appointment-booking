package capitec.branch.appointment.kafka.domain;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EventToJSONMapper {

    public static ObjectMapper getMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return objectMapper;
    }
}