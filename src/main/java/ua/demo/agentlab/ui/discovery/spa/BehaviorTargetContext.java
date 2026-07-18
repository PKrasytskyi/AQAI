package ua.demo.agentlab.ui.discovery.spa;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the compact or line-oriented target-context contract emitted by requirement normalization. */
public final class BehaviorTargetContext {

    private static final Pattern KEY = Pattern.compile(
            "(?i)(?:^|[\\s;`*])(?<key>pageCapability|componentCapability|sourceRoute|targetRoute|sourcePage|targetPage|logoutAccessMode)\\s*:\\s*",
            Pattern.MULTILINE);

    private final Map<String, String> values;

    private BehaviorTargetContext(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public static BehaviorTargetContext parse(String raw) {
        String source = raw == null ? "" : raw;
        Matcher matcher = KEY.matcher(source);
        Map<String, String> result = new LinkedHashMap<>();
        String activeKey = null;
        int valueStart = -1;
        while (matcher.find()) {
            if (activeKey != null) {
                result.putIfAbsent(activeKey, clean(source.substring(valueStart, matcher.start())));
            }
            activeKey = matcher.group("key").toLowerCase(Locale.ROOT);
            valueStart = matcher.end();
        }
        if (activeKey != null) {
            result.putIfAbsent(activeKey, clean(source.substring(valueStart)));
        }
        return new BehaviorTargetContext(result);
    }

    public String value(String key) {
        return key == null ? "" : values.getOrDefault(key.toLowerCase(Locale.ROOT), "");
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace("`", "")
                .replaceAll("^[\\s;,*]+|[\\s;,*]+$", "")
                .trim();
    }
}
