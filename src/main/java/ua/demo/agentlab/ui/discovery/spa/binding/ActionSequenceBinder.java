package ua.demo.agentlab.ui.discovery.spa.binding;

import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Preserves action order while localizing individual binding failures. */
public final class ActionSequenceBinder {

    public List<BoundSpaBehaviorStep> bind(List<String> actions, StepResolver resolver, List<String> review) {
        if (actions == null || actions.isEmpty()) return List.of();
        List<BoundSpaBehaviorStep> result = new ArrayList<>();
        for (String action : actions) {
            StepResolution resolution = resolver.resolve(action);
            resolution.step().ifPresent(result::add);
            if (!resolution.reason().isBlank()) review.add(resolution.reason());
        }
        return List.copyOf(result);
    }

    @FunctionalInterface
    public interface StepResolver {
        StepResolution resolve(String rawAction);
    }

    public record StepResolution(Optional<BoundSpaBehaviorStep> step, String reason) {
        public static StepResolution bound(BoundSpaBehaviorStep step) {
            return new StepResolution(Optional.of(step), "");
        }

        public static StepResolution unbound(String reason) {
            return new StepResolution(Optional.empty(), reason == null ? "" : reason);
        }
    }
}
