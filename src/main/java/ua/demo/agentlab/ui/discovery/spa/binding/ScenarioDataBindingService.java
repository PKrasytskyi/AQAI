package ua.demo.agentlab.ui.discovery.spa.binding;

import ua.demo.agentlab.core.data.PropertiesTestDataProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves scenario data independently from semantic UI binding. */
public final class ScenarioDataBindingService {
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([^}]+)}");

    public ScenarioDataBindingResult resolve(Map<String, String> rawValues) {
        Map<String, String> source = rawValues == null ? Map.of() : rawValues;
        List<String> review = new ArrayList<>();
        Map<String, String> result = new LinkedHashMap<>();
        String dataset = source.getOrDefault("dataset", "");
        Map<String, String> datasetValues = Map.of();
        String datasetFailure = "";
        if (!dataset.isBlank()) {
            try {
                datasetValues = new PropertiesTestDataProvider().scenarioData(dataset).values();
            } catch (RuntimeException exception) {
                datasetFailure = concise(exception);
            }
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String resolved = resolveVariables(entry.getValue(), datasetValues, review);
            if (!resolved.isBlank()) result.put(entry.getKey(), resolved);
        }
        boolean unresolvedPlaceholder = source.entrySet().stream().filter(entry -> !"dataset".equalsIgnoreCase(entry.getKey()))
                .anyMatch(entry -> VARIABLE.matcher(entry.getValue()).find() && !result.containsKey(entry.getKey()));
        if (!datasetFailure.isBlank() && unresolvedPlaceholder) {
            review.add("Scenario dataset '" + dataset + "' is unavailable and required values were not supplied through ENV/system properties: " + datasetFailure);
        }
        return new ScenarioDataBindingResult(Map.copyOf(result), List.copyOf(review));
    }

    private String resolveVariables(String value, Map<String, String> dataset, List<String> review) {
        Matcher matcher = VARIABLE.matcher(value == null ? "" : value);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String replacement = Optional.ofNullable(System.getenv(key))
                    .orElse(Optional.ofNullable(System.getProperty(key)).orElse(dataset.get(key)));
            if (replacement == null || replacement.isBlank()) {
                review.add("Missing data value for '" + key + "'.");
                return "";
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString().trim();
    }

    private String concise(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName()
                : message.replaceAll("\\s+", " ").trim();
    }

    public record ScenarioDataBindingResult(Map<String, String> values, List<String> reviewReasons) {}
}
