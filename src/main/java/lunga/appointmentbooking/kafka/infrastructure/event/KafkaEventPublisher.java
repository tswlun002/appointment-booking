package lunga.appointmentbooking.kafka.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import lunga.appointmentbooking.kafka.infrastructure.configuration.properties.ProducerProperties;
import lunga.appointmentbooking.kafka.domain.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static lunga.appointmentbooking.kafka.app.EventPublishUseCaseImpl.AnonymousDefaultErrorValue;


@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher<K extends Serializable,V extends EventValue,E extends ErrorEventValue>
        implements EventPublisher<K, V>, CallbackEventPublisher<K,V,E> {

    private  final KafkaTemplate<K, V> kafkaTemplate;
    private final ProducerProperties producerProperties;



    @Override
    public CompletableFuture<PublisherResults<K,V>> publishAsync(K key,V event) {


        CompletableFuture<PublisherResults<K, V>> publisherResultsCompletableFuture;

        Long partition = null;

        if(event instanceof ErrorEventValue errorEventValue){
            partition = errorEventValue.getPartition();
        }
         try {


             var future = partition != null ?
                     kafkaTemplate.send(event.getTopic(), Math.toIntExact(partition), key, event)
                     : kafkaTemplate.send(event.getTopic(), key, event);

             publisherResultsCompletableFuture= future
                     .handleAsync(((result, throwable) ->
                     {

                         if (result == null) return new PublisherResults<>(event, key, null, null, throwable);

                         ProducerRecord<K, V> producerRecord = result.getProducerRecord();
                         RecordMetadata recordMetadata = result.getRecordMetadata();

                         return new PublisherResults<>(producerRecord.value(), producerRecord.key(), (long) recordMetadata.partition(),
                                 recordMetadata.offset(), throwable);
                     }));

         }catch (TimeoutException | KafkaException e){

             PublisherResults<K, V> publisherResults = new PublisherResults<>(event, key, null, null, e);
              publisherResultsCompletableFuture = CompletableFuture.completedFuture(publisherResults);
         }

         return  publisherResultsCompletableFuture;
    }


    public  static   BiFunction<Throwable, Set<Class<? extends Exception>>, Boolean> isInstanceOfRetryableExceptions (){
        return (exception, exceptions) ->
                exceptions
                        .stream()
                        .anyMatch(retryableException -> retryableException.isInstance(exception) ||
                                exception.getCause() != null && retryableException.isInstance(exception.getCause()));
    }

    @Override
    public <I extends Serializable, T extends EventValue> PublisherResults<I, T> callback( final PublisherResults<K, V> results) {

            V event = results.event();

            Throwable throwable = results.exception();

            PublisherResults<I, T> finalResults;

            if(throwable==null){

                if(event instanceof ErrorEventValue errorEventValue){

                    errorEventValue.setRetryCount(errorEventValue.getRetryCount() + 1);
                    errorEventValue.setDeadLetterStatus(DEAD_LETTER_STATUS.RECOVERED);
                    errorEventValue.setPartition(results.partition());
                    errorEventValue.setOffset(results.offset());

                    finalResults = new PublisherResults<>((T)errorEventValue, (I)errorEventValue.getKey(), errorEventValue.getPartition(),
                            errorEventValue.getOffset(), null);
               }

                else{

                    finalResults = new PublisherResults<>((T)event, (I)event.getKey(), results.partition(), results.offset(), null);
                }


            }
            else {


                if(event instanceof ErrorEventValue errorEventValue) {
                    errorEventValue.setRetryCount(errorEventValue.getRetryCount() + 1);
                    finalResults = new PublisherResults<>((T)errorEventValue, (I)errorEventValue.getKey(), errorEventValue.getPartition(),
                            errorEventValue.getOffset(), throwable);

                }
                else {

                    DEAD_LETTER_STATUS status = DEAD_LETTER_STATUS.DEAD;

                    try {
                        if (isInstanceOfRetryableExceptions().apply(throwable, producerProperties.getRetryableExceptions())) {

                            DefaultErrorEventValue defaultErrorEventValue = getAnonymousDefaultErrorValue(results, event, throwable, true, 0, status);
                            finalResults = new PublisherResults<>((T)defaultErrorEventValue, (I)defaultErrorEventValue.getKey(), defaultErrorEventValue.getPartition(),
                                    defaultErrorEventValue.getOffset(), throwable);


                        } else {

                            DefaultErrorEventValue defaultErrorEventValue = getAnonymousDefaultErrorValue(results, event, throwable, false, 0, status);
                            finalResults = new PublisherResults<>((T)defaultErrorEventValue, (I)defaultErrorEventValue.getKey(), defaultErrorEventValue.getPartition(),
                                    defaultErrorEventValue.getOffset(), throwable);
                        }

                    } catch (Exception e) {

                        log.error("Failed to save dead letter to database", e);
                        DefaultErrorEventValue defaultErrorEventValue = getAnonymousDefaultErrorValue(results, event, throwable, false, 0, status);
                        finalResults = new PublisherResults<>((T)defaultErrorEventValue, (I)defaultErrorEventValue.getKey(), defaultErrorEventValue.getPartition(),
                                defaultErrorEventValue.getOffset(), e);
                    }
                }
            }
            return finalResults;
    }

    private  DefaultErrorEventValue getAnonymousDefaultErrorValue(PublisherResults< K , V> results, V event,Throwable throwable, boolean isRetryable,int retryCount,DEAD_LETTER_STATUS status) {

        Throwable cause = throwable.getCause();
        String exception = cause != null ? cause.getMessage() : throwable.getMessage();
        var causeClass= cause != null ? cause.getClass().getName() :exception.getClass().toString();
        String stackTrace = throwable.getStackTrace()!=null&&throwable.getStackTrace().length!=0 ? Arrays.toString(throwable.getStackTrace()) :
                throwable.fillInStackTrace().toString();

        return AnonymousDefaultErrorValue(
                event.getTopic(), event.getValue(),
                event.getTraceId(), event.getEventId(), event.getPublishTime(),
                results.partition(), results.offset(), event.getKey(), exception,throwable.getClass().getName(),
                causeClass,stackTrace,isRetryable,retryCount,status
        );
    }

}
