package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;

import java.util.List;

/** Performs browser-backed uniqueness and visibility verification for one locator. */
public final class LocatorRuntimeVerifier {

    public TargetedLocatorVerification verify(
            WebDriver driver,
            TargetedLocatorVerification candidate,
            SpaInventoryConfig config
    ) {
        try {
            List<WebElement> matches = driver.findElements(toBy(candidate.strategy(), candidate.value()));
            boolean visible = matches.stream().anyMatch(WebElement::isDisplayed);
            boolean verified = visible && matches.size() == 1;
            double score = verified
                    ? Math.max(candidate.qualityScore(), config.minConfirmedScore())
                    : candidate.qualityScore();
            String reason = verified
                    ? "live browser confirmed exactly one visible candidate"
                    : "live browser count=" + matches.size();
            return result(candidate, verified, reason, score);
        } catch (RuntimeException exception) {
            return result(candidate, false, "live locator lookup failed: " + concise(exception), candidate.qualityScore());
        }
    }

    public By toBy(String strategy, String value) {
        return switch (strategy == null ? "" : strategy.toLowerCase(java.util.Locale.ROOT)) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "xpath" -> By.xpath(value);
            case "class", "classname", "class_name" -> By.className(value);
            case "tag", "tagname", "tag_name" -> By.tagName(value);
            case "linktext", "link_text" -> By.linkText(value);
            case "partiallinktext", "partial_link_text" -> By.partialLinkText(value);
            default -> By.cssSelector(value);
        };
    }

    private TargetedLocatorVerification result(
            TargetedLocatorVerification source,
            boolean verified,
            String reason,
            double score
    ) {
        return new TargetedLocatorVerification(source.pageId(), source.route(), source.pageFingerprintHash(),
                source.componentId(), source.locatorId(), source.elementId(), source.strategy(), source.value(),
                score, verified, reason, source.requirementIds());
    }

    private String concise(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
