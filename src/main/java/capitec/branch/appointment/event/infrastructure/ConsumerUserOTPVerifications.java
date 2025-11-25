package capitec.branch.appointment.event.infrastructure;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import capitec.branch.appointment.otp.domain.OTPService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.kafka.user.UserDefaultErrorEvent;
import capitec.branch.appointment.kafka.user.UserDefaultEvent;


@Slf4j
@RequiredArgsConstructor
@Component
public class ConsumerUserOTPVerifications {

    private final OTPService service;
    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).EMAIL_VERIFIED_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).PASSWORD_UPDATED_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).DELETE_USER_ACCOUNT_EVENT}",
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
