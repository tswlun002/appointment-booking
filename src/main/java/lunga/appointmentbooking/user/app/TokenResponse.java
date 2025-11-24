package lunga.appointmentbooking.user.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class TokenResponse{
        @JsonProperty("access_token")
        private String token;
        @JsonProperty("expires_in")
        private long expiresIn;
        @JsonProperty("refresh_expires_in")
        private long refreshExpiresIn;
        @JsonProperty("refresh_token")
        private String refreshToken;
        @JsonProperty("token_type")
        private String tokenType;
        @JsonProperty("id_token")
        private String idToken;
        @JsonProperty("not-before-policy")
        private int notBeforePolicy;
        @JsonProperty("session_state")
        private String sessionState;
        private Map<String, Object> otherClaims = new HashMap<>();
        @JsonProperty("scope")
        private String scope;
        @JsonProperty("error")
        private String error;
        @JsonProperty("error_description")
        private String errorDescription;
        @JsonProperty("error_uri")
        private String errorUri;

        @Override
        public String toString() {
                return "TokenResponse{" +
                        "token='" + token + '\'' +
                        ", expiresIn=" + expiresIn +
                        ", refreshExpiresIn=" + refreshExpiresIn +
                        ", refreshToken='" + refreshToken + '\'' +
                        ", tokenType='" + tokenType + '\'' +
                        ", idToken='" + idToken + '\'' +
                        ", notBeforePolicy=" + notBeforePolicy +
                        ", sessionState='" + sessionState + '\'' +
                        ", otherClaims=" + otherClaims +
                        ", scope='" + scope + '\'' +
                        ", error='" + error + '\'' +
                        ", errorDescription='" + errorDescription + '\'' +
                        ", errorUri='" + errorUri + '\'' +
                        '}';
        }
}