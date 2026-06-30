package ua.demo.agentlab.orchestration;

import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AgentOrchestrator {

    private final List<WorkflowAgent> agents;

    public AgentOrchestrator(List<WorkflowAgent> agents) {
        if (agents == null) {
            throw new IllegalArgumentException("agents cannot be null");
        }
        this.agents = resolveExecutionPlan(agents);
    }

    public WorkflowState run(WorkflowState state) {
        PipelineArtifactStore artifactStore = PipelineArtifactStore.from(state);
        for (WorkflowAgent agent : agents) {
            if (state.isFailed()) {
                break;
            }

            if (!supports(agent, state, artifactStore)) {
                continue;
            }

            state.addAudit("Start -> " + agent.name());

            try {
                executeAgent(agent, state, artifactStore);
                state.addAudit("Done -> " + agent.name());
            } catch (Exception exception) {
                state.fail("Agent '%s' failed: %s".formatted(agent.name(), exception.getMessage()));
                state.addAudit("Fail -> " + agent.name());
            }
        }

        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean supports(WorkflowAgent agent, WorkflowState state, PipelineArtifactStore artifactStore) {
        if (agent instanceof PipelineAgent pipelineAgent) {
            return pipelineAgent.supports(artifactStore, state);
        }
        throw new IllegalStateException("Workflow agent is not a PipelineAgent: " + agent.name());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeAgent(WorkflowAgent agent, WorkflowState state, PipelineArtifactStore artifactStore) {
        if (agent instanceof PipelineAgent pipelineAgent) {
            Object input = pipelineAgent.inputFrom(artifactStore, state);
            Object output = pipelineAgent.execute(input, WorkflowRunEnvelope.from(state));
            artifactStore.put(pipelineAgent.output(), output);
            pipelineAgent.applyOutput(output, state);
            return;
        }
        throw new IllegalStateException("Workflow agent is not a PipelineAgent: " + agent.name());
    }

    private List<WorkflowAgent> resolveExecutionPlan(List<WorkflowAgent> inputAgents) {
        List<WorkflowAgent> ordered = new ArrayList<>(inputAgents);
        Map<WorkflowAgent, Integer> inputOrder = new LinkedHashMap<>();
        for (int index = 0; index < ordered.size(); index++) {
            WorkflowAgent agent = ordered.get(index);
            if (agent == null) {
                throw new IllegalArgumentException("agents cannot contain null entries");
            }
            inputOrder.put(agent, index);
        }

        Map<WorkflowArtifact, List<WorkflowAgent>> producersByArtifact = new LinkedHashMap<>();
        for (WorkflowAgent agent : ordered) {
            for (WorkflowArtifact artifact : agent.produces()) {
                producersByArtifact.computeIfAbsent(artifact, ignored -> new ArrayList<>()).add(agent);
            }
        }

        Map<WorkflowAgent, Set<WorkflowAgent>> dependencies = new LinkedHashMap<>();
        Map<WorkflowAgent, Set<WorkflowAgent>> dependents = new LinkedHashMap<>();
        for (WorkflowAgent agent : ordered) {
            dependencies.put(agent, new LinkedHashSet<>());
            dependents.put(agent, new LinkedHashSet<>());
        }

        for (WorkflowAgent consumer : ordered) {
            for (WorkflowArtifact required : consumer.requires()) {
                for (WorkflowAgent producer : producersByArtifact.getOrDefault(required, List.of())) {
                    if (producer == consumer) {
                        continue;
                    }
                    dependencies.get(consumer).add(producer);
                    dependents.get(producer).add(consumer);
                }
            }
        }

        List<WorkflowAgent> ready = ordered.stream()
                .filter(agent -> dependencies.get(agent).isEmpty())
                .sorted(agentComparator(inputOrder))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<WorkflowAgent> resolved = new ArrayList<>();

        while (!ready.isEmpty()) {
            WorkflowAgent agent = ready.remove(0);
            resolved.add(agent);
            for (WorkflowAgent dependent : new ArrayList<>(dependents.get(agent))) {
                dependencies.get(dependent).remove(agent);
                if (dependencies.get(dependent).isEmpty() && !resolved.contains(dependent) && !ready.contains(dependent)) {
                    ready.add(dependent);
                    ready.sort(agentComparator(inputOrder));
                }
            }
        }

        if (resolved.size() != ordered.size()) {
            List<String> cycle = dependencies.entrySet().stream()
                    .filter(entry -> !entry.getValue().isEmpty())
                    .map(entry -> entry.getKey().name() + " requires "
                            + entry.getValue().stream().map(WorkflowAgent::name).toList())
                    .toList();
            throw new IllegalStateException("Workflow dependency graph contains a cycle or unresolved dependency: " + cycle);
        }

        return Collections.unmodifiableList(resolved);
    }

    private Comparator<WorkflowAgent> agentComparator(Map<WorkflowAgent, Integer> inputOrder) {
        return Comparator
                .comparingInt((WorkflowAgent agent) -> inputOrder.getOrDefault(agent, Integer.MAX_VALUE))
                .thenComparing(WorkflowAgent::name);
    }
}
