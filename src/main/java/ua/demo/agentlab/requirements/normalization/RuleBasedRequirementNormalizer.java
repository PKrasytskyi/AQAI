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
        List<NormalizedRequirement> structuredRequirements = structuredRequirements(document.source(), lines);
        if (!structuredRequirements.isEmpty()) {
            List<NormalizedRequirement> allRequirements = new ArrayList<>(structuredRequirements);
            allRequirements.addAll(structuredGovernanceRequirements(document.source(), lines));
            allRequirements.sort(Comparator.comparingInt(requirement -> requirement.sourceReference().startLine()));
            return new NormalizedRequirementBundle(
                    document.source(), List.copyOf(allRequirements),
                    List.of("Structured capability-first requirements were normalized one block per REQ id."),
                    List.of()
            );
        }
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

    /** Parses the capability-first requirement fixture without treating every nested bullet as a test case. */
    private List<NormalizedRequirement> structuredRequirements(String source, List<String> lines) {
        List<StructuredRequirement> blocks = new ArrayList<>();
        StructuredRequirement current = null;
        String section = "";
        for (int index = 0; index < lines.size(); index++) {
            String raw = lines.get(index).trim();
            if (raw.startsWith("## Requirement:")) {
                if (current != null) blocks.add(current.finish(index));
                String heading = raw.substring("## Requirement:".length()).trim();
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("^(REQ-[A-Za-z0-9_-]+)\\s*(?:[-:]\\s*)?(.*)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(heading);
                if (!matcher.matches()) { current = null; continue; }
                current = new StructuredRequirement(matcher.group(1).toUpperCase(Locale.ROOT), matcher.group(2).trim(), index + 1);
                section = "";
                continue;
            }
            if (raw.startsWith("## ")) {
                if (current != null) {
                    blocks.add(current.finish(index));
                    current = null;
                }
                section = "";
                continue;
            }
            if (current == null) continue;
            if (raw.startsWith("### ")) { section = cleanHeading(raw); continue; }
            if (!raw.isBlank()) current.add(section, stripMarker(raw));
        }
        if (current != null) blocks.add(current.finish(lines.size()));
        return blocks.stream().map(block -> block.toNormalized(source)).toList();
    }

    private List<NormalizedRequirement> structuredGovernanceRequirements(String source, List<String> lines) {
        List<NormalizedRequirement> governance = new ArrayList<>();
        String section = "";
        boolean insideExecutableRequirement = false;
        for (int index = 0; index < lines.size(); index++) {
            String raw = lines.get(index).trim();
            if (raw.startsWith("## Requirement:")) {
                insideExecutableRequirement = true;
                continue;
            }
            if (raw.startsWith("## ")) {
                insideExecutableRequirement = false;
                section = cleanHeading(raw);
                continue;
            }
            if (insideExecutableRequirement || !isGovernanceSection(section) || !isRequirementLine(raw)) {
                continue;
            }
            String value = stripMarker(raw);
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("^(GOV-[A-Za-z0-9_-]+)\\s*:\\s*(.+)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(value);
            if (!matcher.matches()) {
                continue;
            }
            String id = matcher.group(1).toUpperCase(Locale.ROOT);
            String statement = matcher.group(2).trim();
            governance.add(new NormalizedRequirement(
                    id,
                    buildTitle(statement),
                    statement,
                    "",
                    true,
                    false,
                    List.of(normalizeTag(section)),
                    new SourceReference(source, index + 1, index + 1, statement)
            ));
        }
        return List.copyOf(governance);
    }

    private boolean isGovernanceSection(String section) {
        String normalized = normalizeTag(section == null ? "" : section);
        return normalized.equals("ui-expectations")
                || normalized.equals("runtime-evidence-expectations")
                || normalized.equals("page-ownership-expectations")
                || normalized.equals("quality-expectations")
                || normalized.equals("non-functional-requirements")
                || normalized.equals("out-of-scope");
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

    private static final class StructuredRequirement {
        private final String id;
        private final String title;
        private final int startLine;
        private int endLine;
        private final Map<String, List<String>> fields = new LinkedHashMap<>();

        private StructuredRequirement(String id, String title, int startLine) {
            this.id = id;
            this.title = title.isBlank() ? id : title;
            this.startLine = startLine;
        }
        private void add(String section, String value) {
            if (section == null || section.isBlank() || value == null || value.isBlank()) return;
            fields.computeIfAbsent(section.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(value.trim());
        }
        private StructuredRequirement finish(int endLine) { this.endLine = Math.max(startLine, endLine); return this; }
        private NormalizedRequirement toNormalized(String source) {
            String capability = join("capability");
            String action = join("action");
            String context = join("target context");
            String expected = join("expected result");
            String assertions = join("assertion requirements");
            String data = join("data requirements");
            String statement = String.join(" ", List.of(
                    "Capability: " + capability + ".",
                    "Action: " + action + ".",
                    "Target Context: " + context + ".",
                    "Assertion Requirements: " + assertions + ".",
                    "Data Requirements: " + data + "."
            )).replaceAll("\\s+", " ").trim();
            List<String> tags = new ArrayList<>();
            tags.add("structured-requirement");
            tags.add("functional-requirements");
            if (!capability.isBlank()) tags.add("capability-" + normalize(capability));
            if (!context.isBlank()) tags.add("target-context");
            fields.getOrDefault("assertion requirements", List.of()).stream()
                    .map(StructuredRequirement::assertionType)
                    .filter(value -> !value.isBlank())
                    .map(value -> "assertion-" + value)
                    .distinct().forEach(tags::add);
            return new NormalizedRequirement(id, title, statement, expected, true, false, tags,
                    new SourceReference(source, startLine, endLine, title), typedAssertions(source), fields);
        }
        private String join(String section) { return String.join(" ", fields.getOrDefault(section, List.of())).trim(); }
        private static String assertionType(String value) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("type\\s*:\\s*([A-Za-z_]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(value == null ? "" : value);
            return matcher.find() ? normalize(matcher.group(1)) : "";
        }
        private List<ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement> typedAssertions(String source) {
            List<ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement> result = new ArrayList<>();
            String type = "", target = "", expected = "";
            for (String value : fields.getOrDefault("assertion requirements", List.of())) {
                String normalized = value.replace("`", "").trim();
                if (normalized.toLowerCase(Locale.ROOT).startsWith("type:")) {
                    if (!type.isBlank()) result.add(new ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement(type, target, expected, new SourceReference(source, startLine, endLine, type)));
                    type = normalized.substring("type:".length()).trim(); target = ""; expected = "";
                } else if (normalized.toLowerCase(Locale.ROOT).startsWith("target:")) {
                    target = normalized.substring("target:".length()).trim();
                } else if (normalized.toLowerCase(Locale.ROOT).startsWith("expectedvalue:")) {
                    expected = normalized.substring("expectedvalue:".length()).trim();
                }
            }
            if (!type.isBlank()) result.add(new ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement(type, target, expected, new SourceReference(source, startLine, endLine, type)));
            return List.copyOf(result);
        }
        private static String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", ""); }
    }
}
