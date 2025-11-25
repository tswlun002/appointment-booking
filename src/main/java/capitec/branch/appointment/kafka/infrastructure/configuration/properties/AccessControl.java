package capitec.branch.appointment.kafka.infrastructure.configuration.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessControl {
    private boolean enable=true;
    private String trustStoreLocation;
    private String trustStorePassword;
    private String saslMechanism="AWS_MSK_IAM";
    private String securityProtocol="SASL_SSL";
    private String jaasConfig="software.amazon.msk.iam.IAMLoginModule required;";
    private String saslCallbackHandlerClass="software.amazon.msk.auth.iam.IAMClientCallbackHandler";

}
