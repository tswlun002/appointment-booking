package lunga.appointmentbooking.user.infrastructure.keycloak;

public enum AuthType {
    SECRET("secret"),
    PASSWORD("password"),
    TOTP("totp"),
    HOTP("hotp"),
    KERBEROS("kerberos");

   private  final String authType;

   AuthType(String type) {
        this.authType=type;
   }
   private String getAuthType() {
         return authType;
   }
   public static AuthType fromAuthType(String authType) {
         authType = authType.toLowerCase();
         for (AuthType auth : AuthType.values()) {
             if (auth.getAuthType().equals(authType)) {
                 return auth;
             }
         }
         throw new IllegalArgumentException("Invalid auth type: " + authType);
   }
}
