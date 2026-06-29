package ua.demo.agentlab.requirements.normalization;

import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;

import java.util.*;

public class RuleBasedRequirementNormalizer implements RequirementNormalizer {

    @Override
    public NormalizedRequirementBundle normalize(RequirementDocument document) {

        List<String> lines = Arrays.asList(document.content().split("\\R", -1));
        List<NormalizedRequirement> requirements = new ArrayList<>();
        List<String> assumptions = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        String currentSection = null;

        for (int index = 0; index < lines.size(); index++) {
            String raw = lines.get(index).trim();

            if (raw.isBlank()) {
                continue;
            }

            if(isHeading(raw)){
                currentSection = cleanHeading(raw);
                continue;
            }

            if (isRequirementLine(raw)) {
                if (isExcludedSection(currentSection)) {
                    continue;
                }
                String statement = stripMarker(raw);

                requirements.add(toRequirement(
                        document.source(),
                        statement,
                        currentSection,
                        index + 1,
                        requirements.size() + 1));

            }
        }

        if (requirements.isEmpty()) {
            currentSection = null;

            for (int index = 0; index < lines.size(); index++) {
                String raw = lines.get(index).trim();

                if (raw.isBlank()) {
                    continue;
                }

                if(isHeading(raw)){
                    currentSection = cleanHeading(raw);
                    continue;
                }

                if(raw.length() > 20) {
                    if (isExcludedSection(currentSection)) {
                        continue;
                    }
                    requirements.add(toRequirement(
                            document.source(),
                            raw,
                            currentSection,
                            index + 1,
                            requirements.size() +1
                    ));
                }
            }
        }

        if (requirements.isEmpty()) {
            risks.add("No explicit requirements statements were detected during normalization");
        }

        assumptions.add("Normalization is currently rule-based and depends on bullets, numbered items, headings, and meaningful text lines.");

        return new NormalizedRequirementBundle(
                document.source(),
                requirements,
                assumptions,
                risks
        );
    }

    private boolean isHeading(String raw){
        return raw.startsWith("#");
    }

    private String cleanHeading(String raw){
        return raw.replace("#", "").trim();
    }

    private boolean isExcludedSection(String currentSection) {
        if (currentSection == null || currentSection.isBlank()) {
            return false;
        }

        String normalized = normalizeTag(currentSection);
        return normalized.equals("out-of-scope")
                || normalized.equals("out-of-scope-items")
                || normalized.equals("out-of-scope-requirements")
                || normalized.equals("non-goals");
    }

    private boolean isRequirementLine(String raw) {

        return raw.startsWith("-") || raw.startsWith("*") || raw.matches("^\\d+[.)].*");
    }

    private String stripMarker(String raw) {

        if (raw.startsWith("-") || raw.startsWith("*")) {

            return raw.substring(1).trim();
        }

        return raw.replaceFirst("^\\d+[.)]\\s*", "").trim();
    }

    private NormalizedRequirement toRequirement(String source, String statement, String currentSection, int line, int sequence) {

        String lower = statement.toLowerCase(Locale.ROOT);

        boolean uiRelevant = containsAny(lower, "screen", "page", "button", "field", "click", "ui", "form", "dialog", "login");
        boolean apiRelevant = containsAny(lower, "api", "endpoint", "request", "response", "status code", "payload", "token", "header");

        if (!uiRelevant && !apiRelevant) {
            uiRelevant = true;
            apiRelevant = true;
        }

        return new NormalizedRequirement(
                "REQ-" + String.format("%03d", sequence),
                buildTitle(statement),
                statement,
                isAssertionRequirementSection(currentSection) ? statement : "",
                uiRelevant,
                apiRelevant,
                extractTags(lower, currentSection),
                new SourceReference(source, line, line, statement)
        );
    }

    private boolean isAssertionRequirementSection(String currentSection) {
        if (currentSection == null || currentSection.isBlank()) {
            return false;
        }
        String section = normalizeTag(currentSection);
        return section.equals("assertion-requirements")
                || section.equals("assertions")
                || section.equals("expected-results")
                || section.equals("expected-result");
    }

    private String buildTitle(String statement) {
        String normalized = statement.replace(".", "").trim();

        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80).trim();
    }

    private List<String> extractTags(String lower, String currentSection) {
        Set<String> tags = new LinkedHashSet<>();

        if (currentSection != null && !currentSection.isBlank()) {
            tags.add(normalizeTag(currentSection));
        }

        if (containsAny(lower, "login", "sign in", "authentication", "password", "session")) {
            tags.add("auth");
        }

        if (containsAny(lower, "validation", "invalid", "error", "required")) {
            tags.add("validation");
        }

        if (containsAny(lower, "timeout", "performance", "seconds", "latency")) {
            tags.add("performance");
        }

        if(containsAny(lower, "api", "endpoint", "request", "response", "payload")){
            tags.add("api");
        }

        if (containsAny(lower, "screen", "page", "button", "field", "click", "form")) {
            tags.add("ui");
        }

        if (tags.isEmpty()) {
            tags.add("general");
        }

        return new ArrayList<>(tags);
    }

    private String normalizeTag(String value){
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }

    private boolean containsAny(String value, String... candidates) {

        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
