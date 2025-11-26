package capitec.branch.appointment.user.infrastructure;

import capitec.branch.appointment.user.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
@Mapper(componentModel = "spring")
public class UserMapperReflection {


    private final ObjectMapper objectMapper  = new ObjectMapper();

    /**
     * Maps JSON InputStream to User object using reflection
     * Ignores password field from JSON response
     * @param inputStream The JSON input stream
     * @return User object populated from JSON
     */
    public User mapFromInputStream(InputStream inputStream) {
        if (inputStream == null) {
            log.warn("Attempted to map null input stream");
            return null;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(inputStream);
            return mapFromJsonNode(jsonNode);

        } catch (Exception e) {
            log.error("Error mapping InputStream to User", e);
            throw new RuntimeException("Failed to map InputStream to User", e);
        }
    }

    /**
     * Maps JSON string to User object using reflection
     * Ignores password field from JSON response
     * @param jsonString The JSON string
     * @return User object populated from JSON
     */
    public User mapFromJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            log.warn("Attempted to map null or empty JSON string");
            return null;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return mapFromJsonNode(jsonNode);

        } catch (Exception e) {
            log.error("Error mapping JSON string to User", e);
            throw new RuntimeException("Failed to map JSON to User", e);
        }
    }

    /**
     * Core mapping logic from JSON using reflection
     * Password field is ignored from JSON
     * @param jsonNode The parsed JSON node
     * @return User object
     */
    private User mapFromJsonNode(JsonNode jsonNode) throws Exception {
        // Extract required constructor parameters from JSON
        String email = getJsonFieldValue(jsonNode, "email", String.class);
        String firstname = getJsonFieldValue(jsonNode, "firstname", String.class);
        String lastname = getJsonFieldValue(jsonNode, "lastname", String.class);

        // Use temporary password since JSON response doesn't include it
        String tempPassword = "TempPass123!";

        // Get the User constructor
        Constructor<User> constructor = User.class.getDeclaredConstructor(
                String.class, String.class, String.class, String.class
        );
        constructor.setAccessible(true);

        // Create User instance
        User user = constructor.newInstance(email, firstname, lastname, tempPassword);

        // Set password to null using reflection (ignoring from JSON)
        setPasswordFieldDirectly(user, null);

        // Map remaining fields from JSON
        mapRemainingFieldsFromJson(jsonNode, user);

        log.debug("Successfully mapped JSON to User: {}", email);
        return user;
    }

    /**
     * Extract field value from JSON node with type conversion
     */
    private <T> T getJsonFieldValue(JsonNode jsonNode, String fieldName, Class<T> type) {
        if (!jsonNode.has(fieldName) || jsonNode.get(fieldName).isNull()) {
            return null;
        }

        JsonNode fieldNode = jsonNode.get(fieldName);

        if (type == String.class) {
            return type.cast(fieldNode.asText());
        } else if (type == Boolean.class || type == boolean.class) {
            return type.cast(fieldNode.asBoolean());
        } else if (type == Integer.class || type == int.class) {
            return type.cast(fieldNode.asInt());
        } else if (type == Long.class || type == long.class) {
            return type.cast(fieldNode.asLong());
        } else if (type == Double.class || type == double.class) {
            return type.cast(fieldNode.asDouble());
        }

        return null;
    }

    /**
     * Map remaining fields from JSON to User object
     * Skips constructor fields and password
     */
    private void mapRemainingFieldsFromJson(JsonNode jsonNode, User user) {
        Map<String, Field> userFields = getAllFields(user.getClass());

        jsonNode.fieldNames().forEachRemaining(jsonFieldName -> {
            // Skip constructor parameters and password
            if (jsonFieldName.equals("email") || jsonFieldName.equals("firstname") ||
                    jsonFieldName.equals("lastname") || jsonFieldName.equals("password")) {
                return;
            }

            try {
                if (userFields.containsKey(jsonFieldName)) {
                    Field targetField = userFields.get(jsonFieldName);
                    targetField.setAccessible(true);

                    Object value = getJsonFieldValueByType(jsonNode, jsonFieldName, targetField.getType());

                    if (value != null) {
                        targetField.set(user, value);
                        log.debug("Mapped JSON field {} with value: {}", jsonFieldName, value);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not map JSON field {}: {}", jsonFieldName, e.getMessage());
            }
        });
    }

    /**
     * Get field value from JSON based on target field type
     */
    private Object getJsonFieldValueByType(JsonNode jsonNode, String fieldName, Class<?> fieldType) {
        if (!jsonNode.has(fieldName) || jsonNode.get(fieldName).isNull()) {
            return null;
        }

        JsonNode fieldNode = jsonNode.get(fieldName);

        if (fieldType == String.class) {
            return fieldNode.asText();
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return fieldNode.asBoolean();
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return fieldNode.asInt();
        } else if (fieldType == Long.class || fieldType == long.class) {
            return fieldNode.asLong();
        } else if (fieldType == Double.class || fieldType == double.class) {
            return fieldNode.asDouble();
        }

        return null;
    }

    /**
     * Maps UserRepresentation to User using reflection only
     * Bypasses password validation by setting field directly after construction
     */
    public static User mapToUser(UserRepresentation userRep) {
        if (userRep == null) {
            return null;
        }

        try {
            // Extract values from UserRepresentation using reflection
            String email = getFieldValue(userRep, "email", String.class);
            String firstname = getFieldValue(userRep, "firstName", String.class);
            String lastname = getFieldValue(userRep, "lastName", String.class);
            String password = getFieldValue(userRep, "password", String.class);

            // Use temporary valid password to bypass constructor validation
            String tempPassword = password != null && !password.isEmpty() ? password : "TempPass123!";

            // Get the User constructor that takes email, firstname, lastname, password
            Constructor<User> constructor = User.class.getDeclaredConstructor(
                    String.class, String.class, String.class, String.class
            );
            constructor.setAccessible(true);

            // Create User instance with temp password to avoid validation
            User user = constructor.newInstance(email, firstname, lastname, tempPassword);

            // Now set the actual password directly using reflection to bypass validation
            //if (password = null) {
            setPasswordFieldDirectly(user, password);
            //}
            setUsernameFromAttributes(userRep, user);
            // Map additional fields using reflection
            mapAdditionalFields(userRep, user);

            return user;

        } catch (Exception e) {
            throw new RuntimeException("Failed to map UserRepresentation to User: " + e.getMessage(), e);
        }
    }
    /**
     * Extract username from attributes Map and set it in User
     */
    private  static  void setUsernameFromAttributes(UserRepresentation userRep, User user) {
        try {
            // Get the attributes field from UserRepresentation
            Field attributesField = findField(userRep.getClass(), "attributes");
            if (attributesField == null) {
                log.error("Warning: attributes field not found in UserRepresentation");
                return;
            }

            attributesField.setAccessible(true);
            Object attributesObj = attributesField.get(userRep);

            if (attributesObj instanceof Map attributes) {
                //Map<String, Object> attributes = (Map<String, Object>) attributesObj;
                Object usernameValue = attributes.get("username");

                if (usernameValue != null) {
                    // Convert username value to long and set in User
                    Long username = convertToLong(usernameValue);
                    if (username != null) {
                        setFieldDirectly(user, "username", username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Warning: Could not extract username from attributes: " + e.getMessage());
        }
    }
    /**
     * Convert various types to Long
     */
    private static Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.error("Cannot convert string '{}' to Long", value);
                return null;
            }
        }
        else if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        else if ( value instanceof LinkedList  list) {
            if(!list.isEmpty()){

                try {
                    return Long.parseLong((String) list.getFirst());
                } catch (NumberFormatException e) {
                    log.error("Cannot convert string '{}' to Long", value);
                    return null;
                }
            }
        }

        log.error("Cannot convert " + value.getClass().getSimpleName() + " to Long");
        return null;
    }

    /**
     * Sets password field directly using reflection to bypass validation annotations
     */
    private static void setPasswordFieldDirectly(User user, String password) {
        try {
            Field passwordField = findField(user.getClass(), "password");
            if (passwordField != null) {
                passwordField.setAccessible(true);
                passwordField.set(user, password);
            }
        } catch (Exception e) {
            log.error("Warning: Could not set password field directly: " + e.getMessage());
        }
    }

    /**
     * Maps additional fields between UserRepresentation and User
     * Skips password field as it's handled separately to avoid validation
     */
    private static void mapAdditionalFields(UserRepresentation source, User target) {
        Map<String, Field> sourceFields = getAllFields(source.getClass());
        Map<String, Field> targetFields = getAllFields(target.getClass());

        for (Map.Entry<String, Field> sourceEntry : sourceFields.entrySet()) {
            String sourceFieldName = sourceEntry.getKey();
            Field sourceField = sourceEntry.getValue();

            // Skip constructor parameters and password (handled separately)
            if (sourceFieldName.equals("email") || sourceFieldName.equals("firstname") ||
                    sourceFieldName.equals("lastname") || sourceFieldName.equals("password")) {
                continue;
            }

            try {
                // Try exact field name match
                if (targetFields.containsKey(sourceFieldName)) {
                    copyFieldValue(source, target, sourceField, targetFields.get(sourceFieldName));
                } else {
                    // Try field name conversion
                    String convertedName = convertFieldName(sourceFieldName);
                    if (targetFields.containsKey(convertedName)) {
                        copyFieldValue(source, target, sourceField, targetFields.get(convertedName));
                    }
                }
            } catch (Exception e) {
                log.error("Warning: Could not map field " + sourceFieldName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Get field value using reflection with type safety
     */
    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, String fieldName, Class<T> expectedType) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field == null) {
                return null;
            }

            field.setAccessible(true);
            Object value = field.get(obj);

            if (value == null) {
                return null;
            }

            if (expectedType.isAssignableFrom(value.getClass())) {
                return (T) value;
            }

            // Try basic type conversion
            return convertValue(value, expectedType);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }

    /**
     * Copy field value from source to target
     */
    private static void copyFieldValue(Object source, Object target, Field sourceField, Field targetField) {
        try {
            sourceField.setAccessible(true);
            targetField.setAccessible(true);

            Object value = sourceField.get(source);

            // Handle type conversion if needed
            if (value != null && !targetField.getType().isAssignableFrom(value.getClass())) {
                value = convertValue(value, targetField.getType());
            }

            targetField.set(target, value);

        } catch (Exception e) {
            log.error("Failed to copy field " + sourceField.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Find field in class hierarchy
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Get all fields from class hierarchy
     */
    private static Map<String, Field> getAllFields(Class<?> clazz) {
        Map<String, Field> fields = new HashMap<>();

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!fields.containsKey(field.getName())) {
                    fields.put(field.getName(), field);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return fields;
    }

    /**
     * Convert field names for specific mappings
     * emailVerified -> verified
     */
    private static String convertFieldName(String fieldName) {
        // Handle specific field name mapping
        if ("emailVerified".equals(fieldName)) {
            return "verified";
        }

        // Handle other potential conversions if needed
        return fieldName;
    }

    /**
     * Basic type conversion
     */
    @SuppressWarnings("unchecked")
    private static <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        // Same type or assignable
        if (targetType.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        // String conversions
        if (targetType == String.class) {
            return (T) value.toString();
        }

        // Number conversions
        if (value instanceof Number num) {
            if (targetType == Long.class || targetType == long.class) {
                return (T) Long.valueOf(num.longValue());
            } else if (targetType == Integer.class || targetType == int.class) {
                return (T) Integer.valueOf(num.intValue());
            } else if (targetType == Double.class || targetType == double.class) {
                return (T) Double.valueOf(num.doubleValue());
            } else if (targetType == Float.class || targetType == float.class) {
                return (T) Float.valueOf(num.floatValue());
            }
        }

        // Boolean conversions
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof String) {
                return (T) Boolean.valueOf(Boolean.parseBoolean((String) value));
            }
        }

        // String to primitive conversions
        if (value instanceof String str) {
            try {
                if (targetType == Long.class || targetType == long.class) {
                    return (T) Long.valueOf(Long.parseLong(str));
                } else if (targetType == Integer.class || targetType == int.class) {
                    return (T) Integer.valueOf(Integer.parseInt(str));
                } else if (targetType == Double.class || targetType == double.class) {
                    return (T) Double.valueOf(Double.parseDouble(str));
                } else if (targetType == Float.class || targetType == float.class) {
                    return (T) Float.valueOf(Float.parseFloat(str));
                } else if (targetType == Boolean.class || targetType == boolean.class) {
                    return (T) Boolean.valueOf(Boolean.parseBoolean(str));
                }
            } catch (NumberFormatException e) {
                log.error("Failed to convert string '{}' to {}", str, targetType.getSimpleName());
            }
        }

        return (T) value;
    }

    /**
     * Alternative method: Create User with minimal constructor and set all fields via reflection
     * This completely bypasses all validation annotations
     */
    protected static User mapToUserBypassValidation(UserRepresentation userRep) {
        if (userRep == null) {
            return null;
        }

        try {
            // Try to create User instance without calling constructor validation
            User user = createUserInstanceWithoutValidation();

            // Set all fields directly using reflection
            setFieldDirectly(user, "email", getFieldValue(userRep, "email", String.class));
            setFieldDirectly(user, "firstname", getFieldValue(userRep, "firstName", String.class));
            setFieldDirectly(user, "lastname", getFieldValue(userRep, "lastname", String.class));
            setFieldDirectly(user, "password", getFieldValue(userRep, "password", String.class));

            // Set other fields with automatic mapping
            mapAdditionalFieldsBypass(userRep, user);

            return user;

        } catch (Exception e) {
            // Fallback to original method if direct instantiation fails
            log.error("Direct instantiation failed, using constructor method: {}", e.getMessage());
            return mapToUser(userRep);
        }
    }

    /**
     * Create User instance without calling constructor (bypasses all validation)
     */
    private static User createUserInstanceWithoutValidation() throws Exception {
        // Use constructor bypassing
        try {
            // Method 1: Try to get no-args constructor
            Constructor<User> noArgsConstructor = User.class.getDeclaredConstructor();
            noArgsConstructor.setAccessible(true);
            return noArgsConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            // Method 2: Use constructor with null values and override fields immediately
            Constructor<User> constructor = User.class.getDeclaredConstructor(
                    String.class, String.class, String.class, String.class
            );
            constructor.setAccessible(true);

            // Create with temporary valid values, will be overwritten
            return constructor.newInstance("temp@temp.com", "temp", "temp", "TempPass123!");
        }
    }

    /**
     * Set field directly bypassing any validation
     */
    private static void setFieldDirectly(User user, String fieldName, Object value) {
        try {
            Field field = findField(user.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(user, value);
            }
        } catch (Exception e) {
            log.error("Warning: Could not set field {}: {}", fieldName, e.getMessage());
        }
    }

    /**
     * Map additional fields bypassing validation
     */
    private static void mapAdditionalFieldsBypass(UserRepresentation source, User target) {
        Map<String, Field> sourceFields = getAllFields(source.getClass());
        Map<String, Field> targetFields = getAllFields(target.getClass());

        for (Map.Entry<String, Field> sourceEntry : sourceFields.entrySet()) {
            String sourceFieldName = sourceEntry.getKey();
            Field sourceField = sourceEntry.getValue();

            // Skip core fields already handled
            if (sourceFieldName.equals("email") || sourceFieldName.equals("firstname") ||
                    sourceFieldName.equals("lastname") || sourceFieldName.equals("password")) {
                continue;
            }

            try {
                // Try exact field name match
                if (targetFields.containsKey(sourceFieldName)) {
                    copyFieldValueBypass(source, target, sourceField, targetFields.get(sourceFieldName));
                } else {
                    // Try field name conversion
                    String convertedName = convertFieldName(sourceFieldName);
                    if (targetFields.containsKey(convertedName)) {
                        copyFieldValueBypass(source, target, sourceField, targetFields.get(convertedName));
                    }
                }
            } catch (Exception e) {
                log.error("Warning: Could not map field {}  : {}" ,sourceFieldName, e.getMessage());
            }
        }
    }

    /**
     * Copy field value bypassing any validation annotations
     */
    private static void copyFieldValueBypass(Object source, Object target, Field sourceField, Field targetField) {
        try {
            sourceField.setAccessible(true);
            targetField.setAccessible(true);

            Object value = sourceField.get(source);

            // Handle type conversion if needed
            if (value != null && !targetField.getType().isAssignableFrom(value.getClass())) {
                value = convertValue(value, targetField.getType());
            }

            // Set field directly without triggering validation
            targetField.set(target, value);

        } catch (Exception e) {
            log.error("Failed to copy field {} : {}" ,sourceField.getName() , e.getMessage());
        }
    }
    protected static java.util.List<User> mapToUserList(java.util.List<UserRepresentation> userRepList) {
        if (userRepList == null) {
            return null;
        }

        java.util.List<User> userList = new java.util.ArrayList<>();
        for (UserRepresentation userRep : userRepList) {
            User user = mapToUser(userRep);
            if (user != null) {
                userList.add(user);
            }
        }

        return userList;
    }
}