package lunga.appointmentbooking.utils;

import jakarta.ws.rs.InternalServerErrorException;

import java.util.Arrays;

public enum AppHttpStatus {
    PROCESSING("100:200"),
    SUCCESS("200:300"),
    REDIRECT("300:400"),
    CLIENT_ERROR("400:500"),
    INTERNAL_ERROR("500");
    private final String codes;
    AppHttpStatus(String codes) {
        this.codes = codes;
    }
    public int getCode() {
        return Integer.parseInt(codes.split(":")[0]);
    }
    public String getCodes() {
        return codes;
    }
    public static AppHttpStatus fromCode(int code) {
        for (AppHttpStatus status : AppHttpStatus.values()) {
            var intStream = Arrays.stream(status.getCodes().split(":")).mapToInt(Integer::parseInt)
                    .sorted().toArray();
            if (code >= intStream[0]&& code<intStream[1]) {
                return status;
            }
        }
        throw new InternalServerErrorException("Invalid AppHttpStatus code: " + code);
    }
}
