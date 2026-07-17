package ua.demo.agentlab.ui.capability;

import java.util.List;

public record UiCapabilityContract(
        String id,
        UiCapabilityKind kind,
        List<String> aliases,
        boolean requiresBrowserAdapter
) {
    public UiCapabilityContract {
        id = id == null ? "" : id.trim();
        kind = kind == null ? UiCapabilityKind.COMPONENT : kind;
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }
}
