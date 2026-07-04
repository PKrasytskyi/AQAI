package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;

import java.util.List;

public record PomContractSpec(
        String schemaVersion,
        PomPageSpec page,
        List<PomLocatorSpec> locators,
        List<PomComponentSpec> components,
        List<PomActionSpec> actions,
        List<PomAssertionSpec> assertions,
        List<String> coverageGaps,
        List<String> rejectedSuggestions
) {
    public PomContractSpec(
            String schemaVersion,
            PomPageSpec page,
            List<PomLocatorSpec> locators,
            List<PomActionSpec> actions,
            List<PomAssertionSpec> assertions,
            List<String> coverageGaps,
            List<String> rejectedSuggestions
    ) {
        this(schemaVersion, page, locators, List.of(), actions, assertions, coverageGaps, rejectedSuggestions);
    }

    public PomContractSpec {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? LlmOutputSchemaVersion.POM_CONTRACT
                : schemaVersion.trim();
        page = page == null ? PomPageSpec.empty() : page;
        locators = locators == null ? List.of() : List.copyOf(locators);
        components = components == null ? List.of() : List.copyOf(components);
        actions = actions == null ? List.of() : List.copyOf(actions);
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
        coverageGaps = coverageGaps == null ? List.of() : List.copyOf(coverageGaps);
        rejectedSuggestions = rejectedSuggestions == null ? List.of() : List.copyOf(rejectedSuggestions);
    }
}
