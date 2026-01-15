package capitec.branch.appointment.event.infrastructure.kafka.consumer;

import capitec.branch.appointment.event.app.port.OTPPort;
import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.kafka.user.UserDefaultErrorEvent;
import capitec.branch.appointment.kafka.user.UserEventValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ConsumerUserOTPVerifications {

    private final OTPPort otpPort;

    @KafkaListener(topics = {
            "#{T(capitec.branch.appointment.event.app.Topics).EMAIL_VERIFIED_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).PASSWORD_UPDATED_EVENT}",
            "#{T(capitec.branch.appointment.event.app.Topics).DELETE_USER_ACCOUNT_EVENT}",
    },
            groupId = "user-otp-verification", autoStartup = "${kafka.listen.auto.start:true}")
    public void onVerifiedUserOTP(ConsumerRecord<String, EventValue> consumerRecord) {
        var event = consumerRecord.value();
        var username = event instanceof UserEventValue user
                ? user.getUsername()
                : ((UserDefaultErrorEvent) event).getUsername();

        log.info("Verified events listener, traceId:{}", event.getTraceId());
        otpPort.verifyOTP(event.getValue(), username);
        log.info("Verified events listener was successful, traceId:{}", event.getTraceId());
    }
}
