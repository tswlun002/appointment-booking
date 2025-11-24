package lunga.appointmentbooking.notification.app;


import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunga.appointmentbooking.notification.domain.ConfirmationEmail;
import lunga.appointmentbooking.notification.domain.Notification;
import lunga.appointmentbooking.notification.domain.OTPEmail;
import lunga.appointmentbooking.notification.domain.SendEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.annotation.Validated;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class EmailSendUseCase   {

    private final SendEmail sendEmail;
    @Value("${mail.username}")
    private String hostEmail;

    @EventListener(OTPEmail.class)
    public void sendOTPEmail(OTPEmail event) throws MessagingException {

        log.info("Sending otp email , traceId:{}", event.traceId());

        var emailTemplateHTML = getEmailTemplate(event.eventType(), event.fullname(), event.OTPCode(), hostEmail);
        String subject = getSubject(event.eventType());
        sendEmail.sendOTPEmail(hostEmail, Set.of(event.email()), subject,emailTemplateHTML, event.traceId());

    }
    private final Function<String,String> readFile =(path)->{

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(path);
        return asString(resource);
    };
    @EventListener(ConfirmationEmail.class)
    public void sendOTPEmail(ConfirmationEmail event) throws MessagingException {

        log.info("Sending confirmation email, traceId:{}", event.traceId());

        var emailTemplateHTML = getEmailTemplate(event.eventType(), event.fullname(),null, hostEmail);
        String subject = getSubject(event.eventType());
        sendEmail.sendOTPEmail(hostEmail, Set.of(event.email()), subject,emailTemplateHTML, event.traceId());


    }

    private  String getSubject(Notification.EventType eventType) {

        return   switch (eventType){
            case REGISTRATION_EVENT -> "Verification email";
            case  EMAIL_VERIFIED_EVENT ->"Email verified";
            case COMPLETE_REGISTRATION_EVENT -> "Welcome VarsityBlock";
            case PASSWORD_RESET_REQUEST_EVENT -> "Password Change Request";
            case PASSWORD_UPDATED_EVENT ->"Important Security Notice – Password Successfully Updated";
            case DELETE_ACCOUNT_REQUEST_EVENT -> "Important Security Notice – Account Deletion Request";
            case DELETE_ACCOUNT_EVENT -> "Delete User Account";
        };

    }

    /**
     *  Replace the placeholder string with value.
     *  Use replacement on otp placeholder because it might have a regex pattern that causes error.
     */
    private  String getEmailTemplate(Notification.EventType eventType, String fullname, String value, String hostEmail) throws MessagingException {

      return   switch (eventType){
          case REGISTRATION_EVENT -> {
                String emailTemplate = readFile.apply("/email/registration.html");
                yield  emailTemplate.replaceAll("user_full_name",fullname).replace("block_otp_var",value).replaceAll("block_email_var", hostEmail) ;
            }
          case  EMAIL_VERIFIED_EVENT -> {
                String emailTemplate = readFile.apply("/email/email_verified.html");
              yield emailTemplate.replaceAll("user_full_name",fullname).replaceAll("block_email_var", hostEmail) ;

          }
          case COMPLETE_REGISTRATION_EVENT -> {
                String emailTemplate = readFile.apply("/email/welcome.html");
                yield emailTemplate.replaceAll("user_full_name",fullname).replaceAll("block_email_var", hostEmail) ;
            }
          case PASSWORD_RESET_REQUEST_EVENT -> {
                String emailTemplate = readFile.apply("/email/password_reset_request.html");
                yield    emailTemplate.replaceAll("user_full_name",fullname).replace("block_otp_var",value).replaceAll("block_email_var", hostEmail) ;
          }
          case PASSWORD_UPDATED_EVENT -> {
                String emailTemplate = readFile.apply("/email/password_updated.html");
                yield  emailTemplate.replaceAll("user_full_name",fullname).replaceAll("block_email_var", hostEmail) ;
          }
          case DELETE_ACCOUNT_REQUEST_EVENT -> {
                String emailTemplate = readFile.apply("/email/delete_user_account_request.html");
                yield emailTemplate.replaceAll("user_full_name",fullname).replace("block_otp_var",value).replaceAll("block_email_var", hostEmail) ;
          }
          case DELETE_ACCOUNT_EVENT -> {
                String emailTemplate = readFile.apply("/email/user_account_deleted.html");
                yield  emailTemplate.replaceAll("user_full_name",fullname).replaceAll("block_email_var", hostEmail) ;
          }

        };

    }
    private String asString(Resource resource){
        try(Reader reader  = new InputStreamReader(resource.getInputStream(), UTF_8)){
            return FileCopyUtils.copyToString(reader);
        }catch (IOException e){
            throw  new UncheckedIOException(e);
        }
    }


}
