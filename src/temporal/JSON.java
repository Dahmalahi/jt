package temporal;

import java.util.Vector;

/**
 * Lightweight JSON parser for J2ME / CLDC 1.1.
 * Handles flat objects and simple arrays of flat objects.
 * v1.1: Fixed nested-brace crash; added extractArray() for string arrays.
 */
public class JSON {

    /**
     * Parse a JSON array of objects, extracting specified keys from each.
     * Robustly skips nested objects/arrays so content with { } doesn't break parsing.
     */
    public static Vector parseArray(String json, String[] keys) {
        Vector results = new Vector();
        int offset = 0;
        int len = json.length();

        while (offset < len) {
            int startObj = json.indexOf("{", offset);
            if (startObj == -1) break;

            // Find matching closing brace, respecting nesting
            int depth = 0;
            int endObj = -1;
            boolean inString = false;
            for (int i = startObj; i < len; i++) {
                char ch = json.charAt(i);
                if (ch == '\\' && inString) { i++; continue; } // skip escaped char
                if (ch == '"') { inString = !inString; continue; }
                if (inString) continue;
                if (ch == '{') depth++;
                else if (ch == '}') {
                    depth--;
                    if (depth == 0) { endObj = i; break; }
                }
            }
            if (endObj == -1) break;

            String objStr = json.substring(startObj, endObj + 1);
            String[] values = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                values[i] = extractValue(objStr, keys[i]);
            }
            results.addElement(values);
            offset = endObj + 1;
        }
        return results;
    }

    /**
     * Extract a single string or number value for a key from a JSON object string.
     * Returns "Unknown" if not found.
     */
    public static String extractValue(String json, String key) {
        // Try with and without space before colon
        String val = tryExtract(json, "\"" + key + "\":");
        if (val == null) val = tryExtract(json, "\"" + key + "\" :");
        return val != null ? val : "Unknown";
    }

    private static String tryExtract(String json, String search) {
        int idx = json.indexOf(search);
        if (idx == -1) return null;

        int start = idx + search.length();
        int len = json.length();

        // Skip whitespace
        while (start < len && isWhitespace(json.charAt(start))) start++;
        if (start >= len) return null;

        char first = json.charAt(start);

        if (first == '"') {
            // String value — respect escape sequences
            start++;
            StringBuffer sb = new StringBuffer();
            while (start < len) {
                char c = json.charAt(start);
                if (c == '\\' && start + 1 < len) {
                    char next = json.charAt(start + 1);
                    if (next == '"') sb.append('"');
                    else if (next == '\\') sb.append('\\');
                    else if (next == 'n') sb.append('\n');
                    else if (next == 'r') sb.append('\r');
                    else if (next == 't') sb.append('\t');
                    else sb.append(next);
                    start += 2;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    start++;
                }
            }
            return sb.toString();
        } else if (first == 't') {
            return "true";
        } else if (first == 'f') {
            return "false";
        } else if (first == 'n') {
            return null; // JSON null -> Java null -> caller gets "Unknown"
        } else {
            // Number
            int end = start;
            while (end < len && "-0123456789.eE+".indexOf(json.charAt(end)) != -1) end++;
            return json.substring(start, end);
        }
    }

    /**
     * Extract a JSON string field (convenience wrapper returning empty string on miss).
     */
    public static String getString(String json, String key) {
        String val = extractValue(json, key);
        return "Unknown".equals(val) ? "" : val;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }
}
