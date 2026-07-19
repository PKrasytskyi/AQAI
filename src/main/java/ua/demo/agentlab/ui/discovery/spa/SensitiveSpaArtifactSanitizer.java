package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Removes runtime credential values while retaining traceable data-source names. */
final class SensitiveSpaArtifactSanitizer {

    List<BoundSpaBehaviorContract> sanitize(List<BoundSpaBehaviorContract> bindings) {
        return (bindings == null ? List.<BoundSpaBehaviorContract>of() : bindings).stream()
                .map(this::sanitize)
                .toList();
    }

    TargetStateBindingBundle sanitize(TargetStateBindingBundle bundle) {
        if (bundle == null) return null;
        return new TargetStateBindingBundle(
                bundle.schemaVersion(),
                bundle.runMetadata(),
                bundle.targets(),
                sanitize(bundle.behaviorBindings()),
                bundle.sourceTrace()
        );
    }

    private BoundSpaBehaviorContract sanitize(BoundSpaBehaviorContract binding) {
        Map<String, String> data = new LinkedHashMap<>();
        binding.resolvedData().forEach((key, value) -> data.put(key,
                "dataset".equalsIgnoreCase(key) ? value : "${" + key + "}"));
        return new BoundSpaBehaviorContract(
                binding.requirementId(), binding.capability(), binding.pageId(), binding.route(), binding.flowId(),
                binding.componentIds(), binding.steps(), binding.assertions(), data, binding.executable(),
                binding.reviewReasons()
        );
    }
}
