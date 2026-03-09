package capitec.branch.appointment.notification.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.exeption.MailSenderException;
import capitec.branch.appointment.notification.domain.ConfirmationEmail;
import capitec.branch.appointment.notification.domain.Notification;
import capitec.branch.appointment.notification.domain.OTPEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.MailSendException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendUserEmailUseCaseTest  extends AppointmentBookingApplicationTests {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    @Test
    @DisplayName("Send Password Reset Correct Email Template")
    public void send_Password_Reset_Email()  {
        var passResetEvent = new OTPEmail(
                "AhmadMiao1@gmail.com",
                "Ahmad Miao",
                "#2151bc",
                "af13dccf-3596-465f-ae5c-c1bdaf256502",
                Notification.UserEventType.PASSWORD_RESET_REQUEST_EVENT
        );

       assertThatThrownBy(()-> applicationEventPublisher.publishEvent(passResetEvent))
       .isInstanceOf(MailSenderException.class);
    }


    @Test
    @DisplayName("Send Confirm Password Reseted Correct Email Template")
    public void send_Confirm_Password_Reseted_Email()  {
        var passResetEvent = new ConfirmationEmail(
                "AhmadMiao1@gmail.com",
                "Ahmad Miao",
                "af13dccf-3596-465f-ae5c-c1bdaf256502",
                LocalDateTime.now(),
                Notification.UserEventType.PASSWORD_UPDATED_EVENT
        );

        assertThatThrownBy(()-> applicationEventPublisher.publishEvent(passResetEvent))
                .isInstanceOf(MailSenderException.class)
                .hasCauseInstanceOf(MailSendException.class)
                .hasRootCauseInstanceOf(java.net.UnknownHostException.class);

    }

}