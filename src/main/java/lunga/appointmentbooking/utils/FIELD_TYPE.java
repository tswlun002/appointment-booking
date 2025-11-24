package lunga.appointmentbooking.utils;

import jakarta.ws.rs.InternalServerErrorException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@Getter
public enum FIELD_TYPE {
    NAME("firstname:lastname"),
    USERNAME("username:userName:user_name"),
    EMAIL("email"),
    PASSWORD("password"),
    DEPENDENCY("accountid:accountId:account_id:email:user_id:userid"),
    IMAGE_URL("imageurl:image_url:image");
    private final String fieldName;
    FIELD_TYPE(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Get field type by name
     * @param fieldName is the name of field we try to get
     * @return Field Type
     * @throws InternalServerErrorException when param field name null
     * Or the field name does not match any field type
     */
    public static FIELD_TYPE fromFieldName(String fieldName) {
        if(fieldName == null){
            log.error("fieldName is null");
            throw new InternalServerErrorException("fieldName cannot be null");
        }
        fieldName = fieldName.toLowerCase();
        for (FIELD_TYPE value : FIELD_TYPE.values()) {
            if (Arrays.asList(value.getFieldName().split(":")).contains(fieldName)) {
                return value;
            }
        }
        log.error("fieldName:{} is not a valid field type", fieldName);
        //throw new InternalServerErrorException("fieldName " + fieldName + " is not a valid field type");
        return null;
    }
}
