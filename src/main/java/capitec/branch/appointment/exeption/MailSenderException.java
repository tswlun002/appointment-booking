package capitec.branch.appointment.exeption;


public class MailSenderException extends RuntimeException {
    public MailSenderException(String s, Exception e) {
        super(s, e);
    }
}
