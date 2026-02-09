package fraud;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
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
                Map<String, String> map = parseJsonObject(json);

                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String fieldName = entry.getKey();
                    String value = entry.getValue();

                    setFieldValue(obj, fieldName, value);
                }
            }

            return obj;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> parseJsonObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        String currentKey = null;
        boolean inString = false;
        int braceCount = 0;
        int bracketCount = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{')
                    braceCount++;
                if (c == '}')
                    braceCount--;
                if (c == '[')
                    bracketCount++;
                if (c == ']')
                    bracketCount--;
            }

            if (inString || braceCount > 0 || bracketCount > 0 || (c != ':' && c != ',')) {
                sb.append(c);
            } else if (c == ':') {
                currentKey = sb.toString().trim();
                sb = new StringBuilder();
            } else if (c == ',') {
                String value = sb.toString().trim();
                if (currentKey != null && !currentKey.isEmpty()) {
                    map.put(currentKey, value);
                }
                currentKey = null;
                sb = new StringBuilder();
            }
        }

        if (currentKey != null && !currentKey.isEmpty()) {
            String value = sb.toString().trim();
            map.put(currentKey, value);
        }

        return map;
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
    private static void setFieldValue(Object obj, String fieldName, String value) {
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

    private static Object convertValue(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return unescapeString(value);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
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
