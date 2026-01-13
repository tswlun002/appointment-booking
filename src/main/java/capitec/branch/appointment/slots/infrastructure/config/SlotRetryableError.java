package capitec.branch.appointment.slots.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "slot.generation-errors")
public record SlotRetryableError(
        ArrayList<Class<? extends Throwable>> retryables
) {

    public  Map<Class<? extends Throwable>, Boolean> getRetryables(){
        final    Map<Class<? extends Throwable>, Boolean>  defaultErrors = new HashMap<>( Map.of(
                TransientDataAccessException.class, true,
                SocketTimeoutException.class, true,
                QueryTimeoutException.class, true
        ));
        if(!(retryables ==  null || retryables.isEmpty())){

            for (Class<? extends Throwable> retryable : retryables) {
                defaultErrors.put(retryable, true);
            }

        }
        return defaultErrors;
    }
}
