package temporal;

import java.util.Vector;

public class JSON {
    public static Vector parseArray(String json, String[] keys) {
        Vector results = new Vector();
        int offset = 0;
        while (true) {
            int startObj = json.indexOf("{", offset);
            if (startObj == -1)
                break;
            int endObj = json.indexOf("}", startObj);
            if (endObj == -1)
                break;
            String objStr = json.substring(startObj, endObj);

            String[] values = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                values[i] = extractValue(objStr, keys[i]);
            }
            results.addElement(values);
            offset = endObj + 1;
        }
        return results;
    }

    public static String extractValue(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) {
            search = "\"" + key + "\" :";
            idx = json.indexOf(search);
            if (idx == -1)
                return "Unknown";
        }
        int start = idx + search.length();
        while (start < json.length()
                && (json.charAt(start) == ' ' || json.charAt(start) == '\n' || json.charAt(start) == '\r')) {
            start++;
        }

        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return (end == -1) ? "" : json.substring(start, end);
        } else {
            int end = start;
            while (end < json.length() && "-0123456789.eE".indexOf(json.charAt(end)) != -1) {
                end++;
            }
            return json.substring(start, end);
        }
    }
}
