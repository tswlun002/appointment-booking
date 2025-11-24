package lunga.appointmentbooking.authentication.domain;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;



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
        private  Map<String, Object> otherClaims = new HashMap<>();
        @JsonProperty("scope")
        private String scope;
        @JsonProperty("error")
        private String error;
        @JsonProperty("error_description")
        private String errorDescription;
        @JsonProperty("error_uri")
        private String errorUri;

        public String getToken() {
                return token;
        }

        public void setToken(String token) {
                this.token = token;
        }

        public long getExpiresIn() {
                return expiresIn;
        }

        public void setExpiresIn(long expiresIn) {
                this.expiresIn = expiresIn;
        }

        public long getRefreshExpiresIn() {
                return refreshExpiresIn;
        }

        public void setRefreshExpiresIn(long refreshExpiresIn) {
                this.refreshExpiresIn = refreshExpiresIn;
        }

        public String getRefreshToken() {
                return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
                this.refreshToken = refreshToken;
        }

        public String getTokenType() {
                return tokenType;
        }

        public void setTokenType(String tokenType) {
                this.tokenType = tokenType;
        }

        public String getIdToken() {
                return idToken;
        }

        public void setIdToken(String idToken) {
                this.idToken = idToken;
        }

        public int getNotBeforePolicy() {
                return notBeforePolicy;
        }

        public void setNotBeforePolicy(int notBeforePolicy) {
                this.notBeforePolicy = notBeforePolicy;
        }

        public String getSessionState() {
                return sessionState;
        }

        public void setSessionState(String sessionState) {
                this.sessionState = sessionState;
        }
        @JsonAnySetter
        public Map<String, Object> getOtherClaims() {
                return otherClaims;
        }
        @JsonAnySetter
        public void setOtherClaims(Map<String, Object> otherClaims) {
                this.otherClaims = otherClaims;
        }

        public String getScope() {
                return scope;
        }

        public void setScope(String scope) {
                this.scope = scope;
        }

        public String getError() {
                return error;
        }

        public void setError(String error) {
                this.error = error;
        }

        public String getErrorDescription() {
                return errorDescription;
        }

        public void setErrorDescription(String errorDescription) {
                this.errorDescription = errorDescription;
        }

        public String getErrorUri() {
                return errorUri;
        }

        public void setErrorUri(String errorUri) {
                this.errorUri = errorUri;
        }

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