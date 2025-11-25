package capitec.branch.appointment.notification.domain;


import capitec.branch.appointment.exeption.MailSenderException;

import java.util.Set;

public interface SendEmail {

    void sendOTPEmail(String hostEmail, Set<String> recipients,String subject, String emailTemplate, String traceId) throws MailSenderException;
}
