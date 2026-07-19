package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Captures a compact state signature after login and after safe live interactions. */
public class SpaStateSnapshotCaptureService {
    private static final String LOADING_SCRIPT = """
            const visible = e => { const s=getComputedStyle(e), r=e.getBoundingClientRect();
              return s.display !== 'none' && s.visibility !== 'hidden' && r.width > 0 && r.height > 0; };
            return [...document.querySelectorAll('[aria-busy="true"],[role="progressbar"],[class*=loading i],[class*=spinner i]')].filter(visible).length > 0;
            """;

    public UiStateSnapshot capture(WebDriver driver, ProjectProfile profile, UiInteractionPage page) {
        return capture(driver, profile, page == null ? "" : page.pageId(),
                page == null ? null : page.runMetadata(), page == null ? List.of() : page.components());
    }

    public UiStateSnapshot capture(WebDriver driver, ProjectProfile profile, String pageId,
                                   ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata metadata) {
        return capture(driver, profile, pageId, metadata, List.of());
    }

    private UiStateSnapshot capture(WebDriver driver, ProjectProfile profile, String pageId,
                                    ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata metadata,
                                    List<SemanticComponentInventory> components) {
        String route = route(driver);
        List<String> visible = new ArrayList<>();
        List<String> overlays = new ArrayList<>();
        List<String> menus = new ArrayList<>();
        List<String> modals = new ArrayList<>();
        for (SemanticComponentInventory component : components) {
            if (!componentVisible(driver, component)) continue;
            visible.add(component.componentId());
            String signature = component.componentId() + ":" + component.type().name();
            if (component.type() == ComponentType.USER_MENU) menus.add(signature);
            if (component.type() == ComponentType.MODAL) modals.add(signature);
            if (component.type() == ComponentType.MODAL || component.type() == ComponentType.USER_MENU) overlays.add(signature);
        }
        boolean loading = loading(driver);
        boolean authenticated = profile != null && !RouteCanonicalizer.routeEqualsOrSuffix(route, profile.loginRoute());
        List<String> orderedVisible = sorted(visible);
        List<String> orderedOverlays = sorted(overlays);
        List<String> orderedMenus = sorted(menus);
        List<String> orderedModals = sorted(modals);
        String fingerprintSeed = String.join("|", route, String.join(",", orderedVisible), String.join(",", orderedOverlays),
                String.valueOf(loading), String.valueOf(authenticated));
        String fingerprint = sha256(fingerprintSeed);
        return new UiStateSnapshot("state-" + fingerprint.substring(0, 16), pageId, route, fingerprint,
                metadata, orderedVisible, orderedOverlays, orderedMenus, orderedModals,
                loading, !loading, authenticated, loading ? 0.70d : 0.95d,
                List.of("spa-state-snapshot:live-browser", "network-idle=" + !loading));
    }

    private boolean componentVisible(WebDriver driver, SemanticComponentInventory component) {
        if (driver == null || component == null) return false;
        String value = component.rootLocatorValue();
        if (value == null || value.isBlank()) {
            return component.locators().stream().anyMatch(locator -> anyVisible(driver, locator.strategy(), locator.value()));
        }
        return anyVisible(driver, component.rootLocatorStrategy(), value);
    }

    private boolean anyVisible(WebDriver driver, String strategy, String value) {
        try {
            return driver.findElements(by(strategy, value)).stream().anyMatch(WebElement::isDisplayed);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean loading(WebDriver driver) {
        try {
            Object value = ((JavascriptExecutor) driver).executeScript(LOADING_SCRIPT);
            return Boolean.TRUE.equals(value);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String route(WebDriver driver) {
        try { return driver == null ? "" : RouteCanonicalizer.canonicalize(driver.getCurrentUrl()); }
        catch (RuntimeException ignored) { return ""; }
    }

    private By by(String strategy, String value) {
        return switch (strategy == null ? "" : strategy.toLowerCase(Locale.ROOT)) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "xpath" -> By.xpath(value);
            default -> By.cssSelector(value);
        };
    }

    private List<String> sorted(List<String> values) {
        return values.stream().filter(value -> value != null && !value.isBlank()).sorted(Comparator.naturalOrder()).toList();
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : bytes) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
