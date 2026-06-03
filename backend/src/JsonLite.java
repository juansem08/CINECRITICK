import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser JSON mínimo para {"username":"...","password":"..."}.
 * (Sin librerías externas por simplicidad del entregable.)
 */
public final class JsonLite {
    private JsonLite() {}

    public static String getString(String json, String key) {
        if (json == null || key == null) return "";
        String k = Pattern.quote(key);
        // Acepta valor en string o literal null
        Pattern p = Pattern.compile("\"" + k + "\"\\s*:\\s*(?:\"(.*?)\"|null)", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) return "";
        String val = m.group(1);
        if (val == null) return ""; // Era null
        return unescape(val);
    }

    public static int getInt(String json, String key, int fallback) {
        if (json == null || key == null) return fallback;
        String k = Pattern.quote(key);
        Pattern p = Pattern.compile("\"" + k + "\"\\s*:\\s*(-?\\d+|null)", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) return fallback;
        String val = m.group(1);
        if ("null".equals(val)) return fallback;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean hasKey(String json, String key) {
        if (json == null || key == null) return false;
        String k = Pattern.quote(key);
        Pattern p = Pattern.compile("\"" + k + "\"\\s*:", Pattern.DOTALL);
        return p.matcher(json).find();
    }

    private static String unescape(String s) {
        String res = s
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
        
        Matcher m = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(res);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
