package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.*;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Actionable review queue for rejected SPA locator/action/postcondition evidence. */
public final class SpaEvidenceNeedsReviewWriter {
    public String write(UiInteractionInventory inventory, SpaTargetedVerificationResult verification) {
        try {
            List<ReviewItem> items = new ArrayList<>();
            if (verification != null) {
                for (TargetedLocatorVerification locator : verification.locatorVerifications()) {
                    if (!locator.verified()) items.add(new ReviewItem("LOCATOR", locator.pageId(), locator.componentId(), locator.locatorId(), locator.requirementIds(), locator.reason(), recommendation(locator.reason()), List.of("strategy=" + locator.strategy(), "value=" + locator.value(), "score=" + locator.qualityScore())));
                }
                for (TargetedActionVerification action : verification.actionVerifications()) {
                    if (!action.verified()) items.add(new ReviewItem("ACTION", action.pageId(), action.componentId(), action.actionId(), action.requirementIds(), action.reason(), "Confirm every prerequisite locator and replay the action through live targeted verification.", List.of("intent=" + action.intent(), "targetElement=" + action.targetElementId())));
                }
            }
            Path path = Path.of("target", "ai-run", "need-review", "spa-evidence-needs-review.json");
            Files.createDirectories(path.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(path.toFile(), new ReviewQueue(items.size(), items));
            return path.toAbsolutePath().toString();
        } catch (Exception exception) { throw new IllegalStateException("Failed to write SPA evidence review queue", exception); }
    }
    private String recommendation(String reason) {
        String value = reason == null ? "" : reason.toLowerCase();
        if (value.contains("not unique")) return "Add a stable component root or data-testid/aria locator, then repeat browser count validation.";
        if (value.contains("stable")) return "Repeat discovery until evidence is stable, or provide a stable attribute such as data-testid, id, name, or role.";
        if (value.contains("score")) return "Prefer data-testid, stable id, name, or role/accessibility evidence over hierarchy selectors.";
        if (value.contains("risk")) return "Remove external, hidden, absolute XPath, or dynamic-class evidence from this capability.";
        return "Inspect the discovery DOM/screenshot, correct the requirement capability, or add a stable locator hint.";
    }
    public record ReviewQueue(int count, List<ReviewItem> items) { public ReviewQueue { items = items == null ? List.of() : List.copyOf(items); } }
    public record ReviewItem(String type, String pageId, String componentId, String evidenceId, List<String> requirementIds, String reason, String recommendation, List<String> evidence) {
        public ReviewItem { requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds); evidence = evidence == null ? List.of() : List.copyOf(evidence); }
    }
}
