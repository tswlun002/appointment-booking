package capitec.branch.appointment.kafka.infrastructure.configuration;

import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.sharekernel.EventToJSONMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public class CustomSerializer<K,V> implements Serializer<EventValue<K,V>> {
    private final ObjectMapper objectMapper = EventToJSONMapper.getMapper();


    @Override
    public byte[] serialize(String topic, EventValue<K,V> data) {
        try {
            if (data == null){
                System.out.println("Null received at serializing");
                return null;
            }
            System.out.println("Serializing...");
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error when serializing MessageDto to byte[]");
        }
    }

}