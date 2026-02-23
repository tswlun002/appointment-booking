package capitec.branch.appointment.notification.domain;

public record EmailTemplate(
        String hostEmail,
        String toEmail,
        String emailSubject,
        String emailContent
) {
}
