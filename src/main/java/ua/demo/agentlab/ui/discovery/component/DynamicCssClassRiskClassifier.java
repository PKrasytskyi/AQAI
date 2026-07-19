package ua.demo.agentlab.ui.discovery.component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects generated class hashes without penalizing stable product class names. */
public final class DynamicCssClassRiskClassifier {

    private static final Pattern CLASS_TOKEN = Pattern.compile("\\.([a-zA-Z0-9_-]+)");

    public boolean isDynamic(String selector) {
        Matcher matcher = CLASS_TOKEN.matcher(selector == null ? "" : selector);
        while (matcher.find()) {
            String token = matcher.group(1).toLowerCase(Locale.ROOT);
            if (generatedPrefix(token) || mixedHashSuffix(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean generatedPrefix(String token) {
        return token.matches("(?:css|sc|jss)-(?=[a-z0-9_-]*[a-z])(?=[a-z0-9_-]*[0-9])[a-z0-9_-]{5,}");
    }

    private boolean mixedHashSuffix(String token) {
        int separator = token.lastIndexOf('-');
        if (separator < 0 || separator == token.length() - 1) return false;
        String suffix = token.substring(separator + 1);
        return suffix.length() >= 6 && suffix.matches("(?=.*[a-z])(?=.*[0-9])[a-z0-9]+");
    }
}
