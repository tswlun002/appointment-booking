package capitec.branch.appointment.kafka.infrastructure.configuration;

import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.sharekernel.EventToJSONMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDeserializer<K,V> implements Deserializer<EventValue<K,V>> {

    private final Logger logger = LoggerFactory.getLogger(CustomDeserializer.class);
    private final ObjectMapper objectMapper = EventToJSONMapper.getMapper();

    @Override
    public EventValue<K,V> deserialize(String topic, byte[] data) {
        try {
            if (data == null){
                System.out.println("Null received at deserializing");
                return null;
            }

            logger.info("Deserializing event ....");

            if (topic.endsWith(".retry")) {
                return objectMapper.readValue(data, new TypeReference<EventValue.EventError<K, V>>() {});
            }
            return objectMapper.readValue(data, new TypeReference<EventValue.OriginEventValue<K, V>>() {});

        } catch (Exception e) {

            logger.info("Failed to deserializing event ....", e);

            throw new SerializationException("Error when deserializing byte[] to MessageDto",e);
        }
    }

}
