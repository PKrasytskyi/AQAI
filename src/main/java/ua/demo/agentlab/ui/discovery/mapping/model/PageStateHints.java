package ua.demo.agentlab.ui.discovery.mapping.model;

public record PageStateHints(
        boolean publicPage,
        boolean requiresAuthentication,
        boolean hasListContainer,
        boolean hasDetailsArea,
        boolean hasForm,
        boolean hasErrorState
) {
}
