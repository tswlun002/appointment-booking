package capitec.branch.appointment.notification.app;

public class EmailTemplates {

    private static final String EMAIL_HEADER = 
        "<!DOCTYPE html>\n" +
        "<html>\n" +
        "<head>\n" +
        "    <meta charset=\"UTF-8\">\n" +
        "    <style>\n" +
        "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n" +
        "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
        "        .header { background-color: #0066cc; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }\n" +
        "        .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }\n" +
        "        .details { background-color: white; padding: 15px; margin: 20px 0; border-left: 4px solid #0066cc; }\n" +
        "        .detail-row { margin: 8px 0; }\n" +
        "        .label { font-weight: bold; color: #555; }\n" +
        "        .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }\n" +
        "        .reference { background-color: #fff3cd; padding: 10px; margin: 15px 0; border-radius: 3px; text-align: center; }\n" +
        "    </style>\n" +
        "</head>\n" +
        "<body>\n" +
        "    <div class=\"container\">\n";

    private static final String EMAIL_FOOTER = 
        "        <div class=\"footer\">\n" +
        "            <p>This is an automated message. Please do not reply to this email.</p>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</body>\n" +
        "</html>";

    // ========================================================================
    // 1. APPOINTMENT BOOKING CONFIRMATION
    // ========================================================================
    
    public static final String BOOKING_CONFIRMATION_SUBJECT = 
        "Appointment Confirmed";
    
    public static final String BOOKING_CONFIRMATION_BODY = 
        EMAIL_HEADER +
        "        <div class=\"header\">\n" +
        "            <h2>Appointment Confirmed</h2>\n" +
        "        </div>\n" +
        "        <div class=\"content\">\n" +
        "            <p>Dear {customerName},</p>\n" +
        "            <p>Your appointment has been confirmed.</p>\n" +
        "            \n" +
        "            <div class=\"details\">\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Service:</span> {serviceType}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Branch:</span> {branchName}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Address:</span> {branchAddress}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Date:</span> {date}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Time:</span> {startTime} - {endTime}</div>\n" +
        "            </div>\n" +
        "            \n" +
        "            <div class=\"reference\">\n" +
        "                <strong>Reference Number:</strong> {referenceNumber}\n" +
        "            </div>\n" +
        "            \n" +
        "            <p>Please arrive 5 minutes early.</p>\n" +
        "            <p>Need to reschedule? Visit our website or call us.</p>\n" +
        "            \n" +
        "            <p>Best regards,<br>{branchName}</p>\n" +
        "        </div>\n" +
        EMAIL_FOOTER;

    // ========================================================================
    // 2. APPOINTMENT RESCHEDULED
    // ========================================================================
    
    public static final String RESCHEDULE_CONFIRMATION_SUBJECT = 
        "Appointment Rescheduled";
    
    public static final String RESCHEDULE_CONFIRMATION_BODY = 
        EMAIL_HEADER +
        "        <div class=\"header\">\n" +
        "            <h2>Appointment Rescheduled</h2>\n" +
        "        </div>\n" +
        "        <div class=\"content\">\n" +
        "            <p>Dear {customerName},</p>\n" +
        "            <p>Your appointment has been rescheduled.</p>\n" +
        "            \n" +
        "            <div class=\"details\">\n" +
        "                <div class=\"detail-row\" style=\"margin-top: 15px;\"><span class=\"label\">New Date:</span> {date}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">New Time:</span> {startTime} - {endTime}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Branch:</span> {address}</div>\n" +
        "            </div>\n" +
        "            \n" +
        "            <div class=\"reference\">\n" +
        "                <strong>Reference Number:</strong> {referenceNumber}\n" +
        "            </div>\n" +
        "            \n" +
        "            <p>Please arrive 5 minutes early.</p>\n" +
        "            \n" +
        "            <p>Best regards,<br>{branchName}</p>\n" +
        "        </div>\n" +
        EMAIL_FOOTER;

    // ========================================================================
    // 3. STATUS UPDATE - CHECK-IN
    // ========================================================================
    
    public static final String STATUS_CHECKIN_SUBJECT = 
        "Checked In - {serviceType}";
    
    public static final String STATUS_CHECKIN_BODY = 
        EMAIL_HEADER +
        "        <div class=\"header\" style=\"background-color: #28a745;\">\n" +
        "            <h2>Checked In</h2>\n" +
        "        </div>\n" +
        "        <div class=\"content\">\n" +
        "            <p>Dear {customerName},</p>\n" +
        "            <p>You have been checked in for your appointment.</p>\n" +
        "            \n" +
        "            <div class=\"details\" style=\"border-left-color: #28a745;\">\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Service:</span> {serviceType}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Time:</span> {startTime}</div>\n" +
        "            </div>\n" +
        "            \n" +
        "            <div class=\"reference\">\n" +
        "                <strong>Reference Number:</strong> {referenceNumber}\n" +
        "            </div>\n" +
        "            \n" +
        "            <p>You will be assisted shortly.</p>\n" +
        "            \n" +
        "            <p>Best regards,<br>{branchName}</p>\n" +
        "        </div>\n" +
        EMAIL_FOOTER;

    // ========================================================================
    // 4. STATUS UPDATE - IN PROGRESS
    // ========================================================================
    
    public static final String STATUS_INPROGRESS_SUBJECT = 
        "Appointment In Progress - {serviceType}";
    
    public static final String STATUS_INPROGRESS_BODY = 
        EMAIL_HEADER +
        "        <div class=\"header\" style=\"background-color: #fd7e14;\">\n" +
        "            <h2>Appointment In Progress</h2>\n" +
        "        </div>\n" +
        "        <div class=\"content\">\n" +
        "            <p>Dear {customerName},</p>\n" +
        "            <p>Your appointment is now in progress.</p>\n" +
        "            \n" +
        "            <div class=\"details\" style=\"border-left-color: #fd7e14;\">\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Service:</span> {serviceType}</div>\n" +
        "            </div>\n" +
        "            \n" +
        "            <div class=\"reference\">\n" +
        "                <strong>Reference Number:</strong> {referenceNumber}\n" +
        "            </div>\n" +
        "            \n" +
        "            <p>Thank you for your patience.</p>\n" +
        "            \n" +
        "            <p>Best regards,<br>{branchName}</p>\n" +
        "        </div>\n" +
        EMAIL_FOOTER;

    // ========================================================================
    // 5. STATUS UPDATE - COMPLETED
    // ========================================================================
    
    public static final String STATUS_COMPLETED_SUBJECT = 
        "Appointment Completed";
    
    public static final String STATUS_COMPLETED_BODY = 
        EMAIL_HEADER +
        "        <div class=\"header\" style=\"background-color: #20c997;\">\n" +
        "            <h2>Appointment Completed</h2>\n" +
        "        </div>\n" +
        "        <div class=\"content\">\n" +
        "            <p>Dear {customerName},</p>\n" +
        "            <p>Your appointment has been completed.</p>\n" +
        "            \n" +
        "            <div class=\"details\" style=\"border-left-color: #20c997;\">\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Date:</span> {date}</div>\n" +
        "            </div>\n" +
        "            \n" +
        "            <div class=\"reference\">\n" +
        "                <strong>Reference Number:</strong> {referenceNumber}\n" +
        "            </div>\n" +
        "            \n" +
        "            <p>Thank you for visiting us.</p>\n" +
        "            \n" +
        "            <p>Best regards,<br>{branchName}</p>\n" +
        "        </div>\n" +
        EMAIL_FOOTER;

    // ========================================================================
    // 6. CANCELLATION - BY CUSTOMER
    // ========================================================================
    
    public static final String CANCEL_BY_CUSTOMER_SUBJECT = 
        "Appointment Cancelled";
    
    public static final String CANCEL_BY_CUSTOMER_BODY = 
        EMAIL_HEADER +
        "        <div class=\"header\" style=\"background-color: #6c757d;\">\n" +
        "            <h2>Appointment Cancelled</h2>\n" +
        "        </div>\n" +
        "        <div class=\"content\">\n" +
        "            <p>Dear {customerName},</p>\n" +
        "            <p>Your appointment has been cancelled as requested.</p>\n" +
        "            \n" +
        "            <div class=\"details\" style=\"border-left-color: #6c757d;\">\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Date:</span> {date}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Time:</span> {startTime}</div>\n" +
        "            </div>\n" +
        "            \n" +
        "            <div class=\"reference\">\n" +
        "                <strong>Reference Number:</strong> {referenceNumber}\n" +
        "            </div>\n" +
        "            \n" +
        "            <p>Need to rebook? Visit our website anytime.</p>\n" +
        "            \n" +
        "            <p>Best regards,<br>{branchName}</p>\n" +
        "        </div>\n" +
        EMAIL_FOOTER;

    // ========================================================================
    // 7. CANCELLATION - BY STAFF/ADMIN
    // ========================================================================
    
    public static final String CANCEL_BY_STAFF_SUBJECT = 
        "Appointment Cancelled ";
    
    public static final String CANCEL_BY_STAFF_BODY = 
        EMAIL_HEADER +
        "        <div class=\"header\" style=\"background-color: #dc3545;\">\n" +
        "            <h2>Appointment Cancelled</h2>\n" +
        "        </div>\n" +
        "        <div class=\"content\">\n" +
        "            <p>Dear {customerName},</p>\n" +
        "            <p>We regret to inform you that your appointment has been cancelled.</p>\n" +
        "            \n" +
        "            <div class=\"details\" style=\"border-left-color: #dc3545;\">\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Date:</span> {date}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Time:</span> {startTime}</div>\n" +
        "            </div>\n" +
        "            \n" +
        "            <div class=\"reference\">\n" +
        "                <strong>Reference Number:</strong> {referenceNumber}\n" +
        "            </div>\n" +
        "            \n" +
        "            <p>Please contact us to reschedule at your convenience.</p>\n" +
        "            <p>We apologize for any inconvenience.</p>\n" +
        "            \n" +
        "            <p>Best regards,<br>{branchName}</p>\n" +
        "        </div>\n" +
        EMAIL_FOOTER;

    // ========================================================================
    // GENERIC STATUS CHANGE
    // ========================================================================
    
    /*public static final String GENERIC_STATUS_UPDATE_SUBJECT =
        "Appointment Update -{toState}";
    
    public static final String GENERIC_STATUS_UPDATE_BODY = 
        EMAIL_HEADER +
        "        <div class=\"header\">\n" +
        "            <h2>Appointment Update</h2>\n" +
        "        </div>\n" +
        "        <div class=\"content\">\n" +
        "            <p>Dear {customerName},</p>\n" +
        "            <p>Your appointment status has been updated from strong>{fromState}</strong> to: <strong>{toState}</strong></p>\n" +
        "            \n" +
        "            <div class=\"details\">\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Date:</span> {date}</div>\n" +
        "                <div class=\"detail-row\"><span class=\"label\">Time:</span> {startTime}</div>\n" +
        "            </div>\n" +
        "            \n" +
        "            <div class=\"reference\">\n" +
        "                <strong>Reference Number:</strong> {referenceNumber}\n" +
        "            </div>\n" +
        "            \n" +
        "            <p>Best regards,<br>{branchName}</p>\n" +
        "        </div>\n" +
        EMAIL_FOOTER;*/
}