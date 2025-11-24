package lunga.appointmentbooking.event.infrastructure;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunga.appointmentbooking.otp.domain.OTPService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lunga.appointmentbooking.kafka.domain.EventValue;
import lunga.appointmentbooking.kafka.user.UserDefaultErrorEvent;
import lunga.appointmentbooking.kafka.user.UserDefaultEvent;


@Slf4j
@RequiredArgsConstructor
@Component
public class ConsumerUserOTPVerifications {

    private final OTPService service;
    @KafkaListener(topics = {
            "#{T(lunga.appointmentbooking.event.app.Topics).EMAIL_VERIFIED_EVENT}",
            "#{T(lunga.appointmentbooking.event.app.Topics).PASSWORD_UPDATED_EVENT}",
            "#{T(lunga.appointmentbooking.event.app.Topics).DELETE_USER_ACCOUNT_EVENT}",
    },
            groupId = "user-otp-verification", autoStartup = "${kafka.listen.auto.start:true}")
    public void onVerifiedUserOTP(ConsumerRecord<String, EventValue> consumerRecord)  {
        var event = consumerRecord.value();
        var username=   event instanceof UserDefaultEvent eventValueModel? eventValueModel.getUsername():((UserDefaultErrorEvent)event).getUsername();

        log.info("Verified events listener, traceId:{}", event.getTraceId());
        service.verify(event.getValue(),username);
        log.info("Verified events listener was successful, traceId:{}",event.getTraceId());

   }
}
