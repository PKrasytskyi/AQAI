package ua.demo.agentlab.ui.discovery.evidence.funnel;

import java.util.List;

/** Product fixture metadata kept outside generic discovery policy. */
public record SpaAcceptanceFixture(
        String fixtureId,
        String projectProfile,
        String requirementsFile,
        List<SpaAcceptanceCapability> flowCapabilities,
        List<SpaAcceptanceCapability> supportedCapabilityContracts
) {
    public SpaAcceptanceFixture {
        fixtureId = safe(fixtureId);
        projectProfile = safe(projectProfile);
        requirementsFile = safe(requirementsFile);
        flowCapabilities = flowCapabilities == null ? List.of() : List.copyOf(flowCapabilities);
        supportedCapabilityContracts = supportedCapabilityContracts == null
                ? List.of()
                : List.copyOf(supportedCapabilityContracts);
        if (fixtureId.isBlank()) {
            throw new IllegalArgumentException("fixtureId cannot be blank");
        }
        if (!supportedCapabilityContracts.containsAll(flowCapabilities)) {
            throw new IllegalArgumentException("flow capabilities must be declared as supported contracts");
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
