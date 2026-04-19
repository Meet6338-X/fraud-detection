package fraud;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Simple JSON serialization/deserialization helper.
 * No external dependencies required.
 */
public class JsonHelper {

    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof String) {
            return "\"" + escapeString((String) obj) + "\"";
        }
        if (obj instanceof Number) {
            return obj.toString();
        }
        if (obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Instant) {
            return "\"" + obj.toString() + "\"";
        }
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }
        if (obj instanceof List) {
            return listToJson((List<?>) obj);
        }
        if (obj instanceof Collection) {
            return collectionToJson((Collection<?>) obj);
        }
        if (obj.getClass().isArray()) {
            return arrayToJson(obj);
        }

        return objectToJson(obj);
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {
        json = json.trim();
        if (json.equals("null")) {
            return null;
        }

        if (clazz == String.class) {
            return (T) unescapeString(json.substring(1, json.length() - 1));
        }
        if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(json);
        }
        if (clazz == Long.class || clazz == long.class) {
            return (T) Long.valueOf(json);
        }
        if (clazz == Double.class || clazz == double.class) {
            return (T) Double.valueOf(json);
        }
        if (clazz == Float.class || clazz == float.class) {
            return (T) Float.valueOf(json);
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) Boolean.valueOf(json);
        }
        if (clazz == Instant.class) {
            return (T) Instant.parse(json.substring(1, json.length() - 1));
        }

        return objectFromJson(json, clazz);
    }

    private static String objectToJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        List<String> fields = new ArrayList<>();
        for (Field field : obj.getClass().getDeclaredFields()) {
            fields.add(field.getName());
        }

        for (int i = 0; i < fields.size(); i++) {
            String fieldName = fields.get(i);
            Object value = getFieldValue(obj, fieldName);

            if (i > 0)
                sb.append(",");
            sb.append("\"").append(fieldName).append("\":");
            sb.append(toJson(value));
        }

        sb.append("}");
        return sb.toString();
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (i > 0)
                sb.append(",");
            sb.append("\"").append(escapeString(entry.getKey().toString())).append("\":");
            sb.append(toJson(entry.getValue()));
            i++;
        }

        sb.append("}");
        return sb.toString();
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < list.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(toJson(list.get(i)));
        }

        sb.append("]");
        return sb.toString();
    }

    private static String collectionToJson(Collection<?> collection) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        int i = 0;
        for (Object item : collection) {
            if (i > 0)
                sb.append(",");
            sb.append(toJson(item));
            i++;
        }

        sb.append("]");
        return sb.toString();
    }

    private static String arrayToJson(Object array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(toJson(Array.get(array, i)));
        }

        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T> T objectFromJson(String json, Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            json = json.trim();

            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                Map<String, Object> map = parseJsonObject(json);

                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String fieldName = entry.getKey();
                    Object value = entry.getValue();

                    setFieldValue(obj, fieldName, value);
                }
            }

            return obj;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        String currentKey = null;
        boolean inString = false;
        boolean readingKey = true;
        int braceCount = 0;
        int bracketCount = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue; // Skip the boundary quotes
            }

            if (!inString) {
                if (c == '{') braceCount++;
                if (c == '}') braceCount--;
                if (c == '[') bracketCount++;
                if (c == ']') bracketCount--;
            }

            if (readingKey) {
                if (!inString && c == ':') {
                    currentKey = sb.toString().trim();
                    sb = new StringBuilder();
                    readingKey = false;
                } else {
                    if (inString || (c != ' ' && c != '\n' && c != '\r' && c != '\t')) {
                        sb.append(c);
                    }
                }
            } else {
                if (!inString && braceCount == 0 && bracketCount == 0 && (c == ',' || c == '}' || i == json.length() - 1)) {
                    if (c != ',' && c != '}') sb.append(c);
                    String valueStr = sb.toString().trim();
                    if (!valueStr.isEmpty() || c == ',' || c == '}') {
                        Object value = parseValue(valueStr);
                        if (currentKey != null && !currentKey.isEmpty()) {
                            map.put(currentKey, value);
                        }
                        sb = new StringBuilder();
                        readingKey = true;
                        currentKey = null;
                    }
                } else {
                    if (inString || braceCount > 0 || bracketCount > 0 || (c != ' ' && c != '\n' && c != '\r' && c != '\t')) {
                        sb.append(c);
                    }
                }
            }
        }

        return map;
    }

    private static Object parseValue(String value) {
        value = value.trim();
        if (value.isEmpty() || value.equals("null")) {
            return null;
        }
        if (value.equals("true") || value.equals("false")) {
            return Boolean.parseBoolean(value);
        }
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseJsonObject(value.substring(1, value.length() - 1));
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static Object getFieldValue(Object obj, String fieldName) {
        try {
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method method = obj.getClass().getMethod(getterName);
            return method.invoke(obj);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

            for (Method method : obj.getClass().getMethods()) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    Object paramValue = convertValue(value, paramType);
                    method.invoke(obj, paramValue);
                    return;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            // Ignore
        }
    }

    @SuppressWarnings("unchecked")
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // If the value is already the target type, return it
        if (targetType.isInstance(value)) {
            return value;
        }
        
        // Handle nested objects (Map -> custom object)
        if (value instanceof Map && !targetType.isPrimitive() && targetType != String.class) {
            try {
                Object nestedObj = targetType.getDeclaredConstructor().newInstance();
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                    setFieldValue(nestedObj, entry.getKey(), entry.getValue());
                }
                return nestedObj;
            } catch (Exception e) {
                // Fall through to string conversion
            }
        }
        
        // Handle String conversion
        if (value instanceof String) {
            String strValue = (String) value;
            if (targetType == String.class) {
                return unescapeString(strValue);
            }
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(strValue);
            }
            if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(strValue);
            }
            if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(strValue);
            }
            if (targetType == Float.class || targetType == float.class) {
                return Float.parseFloat(strValue);
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(strValue);
            }
            if (targetType == Instant.class) {
                try {
                    return Instant.parse(strValue);
                } catch (Exception e) {
                    try {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a", Locale.ENGLISH);
                        return LocalDateTime.parse(strValue, dtf).atZone(java.time.ZoneId.systemDefault()).toInstant();
                    } catch (Exception e2) {
                        return null;
                    }
                }
            }
            return strValue;
        }
        
        // Handle numbers
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == Integer.class || targetType == int.class) {
                return num.intValue();
            }
            if (targetType == Long.class || targetType == long.class) {
                return num.longValue();
            }
            if (targetType == Double.class || targetType == double.class) {
                return num.doubleValue();
            }
            if (targetType == Float.class || targetType == float.class) {
                return num.floatValue();
            }
        }
        
        // Handle Boolean
        if (value instanceof Boolean) {
            Boolean boolValue = (Boolean) value;
            if (targetType == Boolean.class || targetType == boolean.class) {
                return boolValue;
            }
        }
        
        return value;
    }

    private static String escapeString(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeString(String str) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '"':
                        sb.append('"');
                        break;
                    default:
                        sb.append(c);
                        break;
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
