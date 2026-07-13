package ua.demo.agentlab.artifactreuse.semantic;

import ua.demo.agentlab.artifactreuse.flow.FlowContract;

import java.util.stream.Collectors;

public class FlowSemanticDocumentBuilder {

    public String build(FlowContract contract) {
        if (contract == null) {
            return "";
        }
        return String.join("\n",
                "flowId: " + contract.flowId(),
                "type: " + contract.type(),
                "source: " + contract.source().pageName() + " " + contract.source().route(),
                "target: " + contract.target().pageName() + " " + contract.target().route(),
                "requiredStates: " + contract.requiresStates(),
                "producedStates: " + contract.producesStates(),
                "actions: " + contract.steps().stream().map(step -> step.action().name()).collect(Collectors.joining(", ")),
                "assertions: " + String.join(" | ", contract.assertions()),
                "requirements: " + String.join(", ", contract.requirementIds()),
                "evidence: " + String.join(" | ", contract.evidence())
        );
    }
}
