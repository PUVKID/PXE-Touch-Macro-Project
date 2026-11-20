package kr.yunseok.touchmacro;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
public final class TextRules {
    private TextRules() {}
    public static String normalize(String s, boolean spaces, boolean ignoreCase) {
        s = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFKC);
        if (spaces) s = s.replaceAll("\\s+", "");
        return ignoreCase ? s.toLowerCase(Locale.ROOT) : s;
    }
    public static boolean matches(String actual, String wanted, String mode, boolean spaces, boolean ignoreCase) {
        if (wanted == null || wanted.trim().isEmpty()) return false;
        String a = normalize(actual, spaces, ignoreCase);
        if ("REGEX".equals(mode)) {
            // Preserve regex syntax and its spaces; only normalize the displayed text.
            int flags = ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
            return Pattern.compile(wanted, flags).matcher(a).find();
        }
        String b = normalize(wanted, spaces, ignoreCase);
        return "EXACT".equals(mode) ? a.equals(b) : a.contains(b);
    }
}
