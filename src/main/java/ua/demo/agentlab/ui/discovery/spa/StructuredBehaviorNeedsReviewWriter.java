package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Small review queue for requirement-to-evidence binding gaps, separate from raw discovery noise. */
public final class StructuredBehaviorNeedsReviewWriter {
    public String write(List<BoundSpaBehaviorContract> bindings) {
        try {
            List<BindingReviewItem> items = (bindings == null ? List.<BoundSpaBehaviorContract>of() : bindings).stream()
                    .filter(binding -> !binding.executable())
                    .map(binding -> new BindingReviewItem(binding.requirementId(), binding.capability(), binding.pageId(), binding.route(),
                            binding.reviewReasons(), recommendation(binding))).toList();
            Path path = Path.of("target", "ai-run", "need-review", "spa-structured-behavior-needs-review.json");
            Files.createDirectories(path.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(path.toFile(), new BindingReviewQueue(items.size(), items));
            return path.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write structured SPA behavior review queue", exception);
        }
    }

    private String recommendation(BoundSpaBehaviorContract binding) {
        String reasons = String.join(" ", binding.reviewReasons()).toLowerCase();
        if (reasons.contains("missing data") || reasons.contains("dataset")) return "Set the named ENV value or ScenarioData dataset, then rerun targeted verification.";
        if (reasons.contains("target route") || reasons.contains("page matches")) return "Run authenticated MODULE_NAVIGATION discovery and confirm the target SPA route before retrying.";
        if (reasons.contains("assertion")) return "Add a typed, observable Assertion Requirement and confirm its component locator in the browser.";
        return "Inspect the bound component/locator candidates, add a stable product locator or test hook, and rerun targeted verification.";
    }

    public record BindingReviewQueue(int count, List<BindingReviewItem> items) {
        public BindingReviewQueue { items = items == null ? List.of() : List.copyOf(items); }
    }
    public record BindingReviewItem(String requirementId, String capability, String pageId, String route,
                                    List<String> reasons, String recommendation) {
        public BindingReviewItem { reasons = reasons == null ? List.of() : List.copyOf(reasons); }
    }
}
