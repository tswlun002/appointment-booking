package capitec.branch.appointment.kafka.infrastructure.configuration.properties;


import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "kafka.security-config")
@Component
@Getter
@Setter
public class SecurityProperties {
    private AccessControl accessControl;


    public Map<String, Object> getSecurityConfig(SECURITY_TYPE_ENUM SECURITYTYPEENUM) {
        return switch (SECURITYTYPEENUM){
            case NONE -> Collections.emptyMap();
            case IAM -> getIAMConfig();
        };
    }

    private Map<String, Object> getIAMConfig() {
        Map<String, Object> config = new HashMap<>();


        if(StringUtils.isNotBlank(accessControl.getTrustStorePassword())){
            config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, accessControl.getTrustStorePassword());
        }
        config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, accessControl.getTrustStoreLocation());
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, accessControl.getSecurityProtocol());
        config.put(SaslConfigs.SASL_MECHANISM, accessControl.getSaslMechanism());
        config.put(SaslConfigs.SASL_JAAS_CONFIG, accessControl.getJaasConfig());
        config.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS,accessControl.getSaslCallbackHandlerClass());

        return config;
    }


}
