package ua.demo.agentlab.ui.discovery.selenium.collector;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.evidence.model.DiscoveredPageEvidence;
import ua.demo.agentlab.ui.discovery.selenium.extractor.FormStructureExtractor;
import ua.demo.agentlab.ui.discovery.selenium.extractor.InteractiveElementExtractor;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredField;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredForm;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredInteractiveElement;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.parser.DomParser;
import ua.demo.agentlab.ui.discovery.selenium.scanner.PageScanner;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PageSnapshotCollector {

    private final InteractiveElementExtractor interactiveElementExtractor;
    private final FormStructureExtractor formStructureExtractor;
    private final PageScanner pageScanner;
    private final DomParser domParser;
    private final RuntimeLocatorCountCollector runtimeLocatorCountCollector;

    public PageSnapshotCollector(
            InteractiveElementExtractor interactiveElementExtractor,
            FormStructureExtractor formStructureExtractor
    ) {
        this(
                interactiveElementExtractor,
                formStructureExtractor,
                new PageScanner(),
                new DomParser(),
                new RuntimeLocatorCountCollector()
        );
    }

    public PageSnapshotCollector(
            InteractiveElementExtractor interactiveElementExtractor,
            FormStructureExtractor formStructureExtractor,
            PageScanner pageScanner,
            DomParser domParser
    ) {
        this(interactiveElementExtractor, formStructureExtractor, pageScanner, domParser, new RuntimeLocatorCountCollector());
    }

    public PageSnapshotCollector(
            InteractiveElementExtractor interactiveElementExtractor,
            FormStructureExtractor formStructureExtractor,
            PageScanner pageScanner,
            DomParser domParser,
            RuntimeLocatorCountCollector runtimeLocatorCountCollector
    ) {
        if (interactiveElementExtractor == null) {
            throw new IllegalArgumentException("interactiveElementExtractor cannot be null");
        }
        if (formStructureExtractor == null) {
            throw new IllegalArgumentException("formStructureExtractor cannot be null");
        }
        if (pageScanner == null) {
            throw new IllegalArgumentException("pageScanner cannot be null");
        }
        if (domParser == null) {
            throw new IllegalArgumentException("domParser cannot be null");
        }
        if (runtimeLocatorCountCollector == null) {
            throw new IllegalArgumentException("runtimeLocatorCountCollector cannot be null");
        }
        this.interactiveElementExtractor = interactiveElementExtractor;
        this.formStructureExtractor = formStructureExtractor;
        this.pageScanner = pageScanner;
        this.domParser = domParser;
        this.runtimeLocatorCountCollector = runtimeLocatorCountCollector;
    }

    public DiscoveredPageSnapshot collect(WebDriver driver, String pageIdHint, DiscoveredPageEvidence evidence) {
        RawPageSnapshot rawPageSnapshot = pageScanner.scan(driver, evidence);
        List<RawElement> rawElements = runtimeLocatorCountCollector.enrich(driver, domParser.parse(rawPageSnapshot));
        String currentUrl = driver.getCurrentUrl();
        String title = driver.getTitle();
        List<String> headings = extractTexts(driver, "h1, h2, h3");
        List<DiscoveredInteractiveElement> links = interactiveElementExtractor.extractLinks(driver);
        List<DiscoveredInteractiveElement> buttons = interactiveElementExtractor.extractButtons(driver);
        List<DiscoveredForm> forms = formStructureExtractor.extractForms(driver);
        boolean modalVisible = hasVisible(driver, "dialog, [role='dialog'], .modal, [aria-modal='true']");
        boolean authenticatedArea = inferAuthenticatedArea(currentUrl, links, buttons);
        List<String> capabilities = inferCapabilities(currentUrl, title, links, buttons, forms, modalVisible, authenticatedArea);
        List<LocatorHint> locatorHints = collectLocatorHints(links, buttons, forms);
        String fingerprint = buildFingerprint(currentUrl, title, headings, capabilities);
        String pageId = (pageIdHint == null || pageIdHint.isBlank())
                ? "page-" + fingerprint.substring(0, Math.min(10, fingerprint.length()))
                : pageIdHint;

        return new DiscoveredPageSnapshot(
                pageId,
                currentUrl,
                title,
                headings,
                links,
                buttons,
                forms,
                modalVisible,
                authenticatedArea,
                capabilities,
                locatorHints,
                fingerprint,
                evidence,
                rawPageSnapshot,
                rawElements
        );
    }

    private List<String> extractTexts(WebDriver driver, String selector) {
        List<String> texts = new ArrayList<>();
        for (WebElement element : driver.findElements(By.cssSelector(selector))) {
            if (!safeDisplayed(element)) {
                continue;
            }

            String text = safeText(element);
            if (text != null && !text.isBlank()) {
                texts.add(text);
            }
        }
        return texts;
    }

    private boolean hasVisible(WebDriver driver, String selector) {
        for (WebElement element : driver.findElements(By.cssSelector(selector))) {
            if (safeDisplayed(element)) {
                return true;
            }
        }
        return false;
    }

    private boolean inferAuthenticatedArea(
            String currentUrl,
            List<DiscoveredInteractiveElement> links,
            List<DiscoveredInteractiveElement> buttons
    ) {
        String url = currentUrl.toLowerCase(Locale.ROOT);
        if (!url.contains("login") && containsAction(links, buttons, "logout", "log out", "sign out")) {
            return true;
        }
        return url.contains("/app") || url.contains("dashboard") || url.contains("overview");
    }

    private List<String> inferCapabilities(
            String currentUrl,
            String title,
            List<DiscoveredInteractiveElement> links,
            List<DiscoveredInteractiveElement> buttons,
            List<DiscoveredForm> forms,
            boolean modalVisible,
            boolean authenticatedArea
    ) {
        Set<String> capabilities = new LinkedHashSet<>();
        String pageText = ((title == null ? "" : title) + " " + currentUrl).toLowerCase(Locale.ROOT);

        if (forms.stream().anyMatch(this::hasPasswordField)) {
            capabilities.add("authentication");
        }
        if (!forms.isEmpty()) {
            capabilities.add("form-submit");
        }
        if (modalVisible) {
            capabilities.add("modal");
        }
        if (authenticatedArea) {
            capabilities.add("authenticated-area");
        }
        if (containsAction(links, buttons, "forgot", "reset", "recover", "lookup")) {
            capabilities.add("recovery-entry");
        }
        if (containsAction(links, buttons, "details", "view", "record")) {
            capabilities.add("details-navigation");
        }
        if (containsAction(links, buttons, "logout", "log out", "sign out")) {
            capabilities.add("logout");
        }
        if (hasVisibleCount(links) + hasVisibleCount(buttons) >= 2) {
            capabilities.add("navigation");
        }
        if (hasStructure(currentUrl, pageText, "table", "overview", "dashboard", "list")) {
            capabilities.add("overview");
        }
        if (hasStructure(currentUrl, pageText, "security", "challenge", "verification")) {
            capabilities.add("security");
        }
        if (hasStructure(currentUrl, pageText, "details", "profile", "transaction")) {
            capabilities.add("details");
        }
        if (hasStructure(currentUrl, pageText, "register", "sign up")) {
            capabilities.add("create");
        }

        if (capabilities.isEmpty()) {
            capabilities.add("generic");
        }

        return new ArrayList<>(capabilities);
    }

    private List<LocatorHint> collectLocatorHints(
            List<DiscoveredInteractiveElement> links,
            List<DiscoveredInteractiveElement> buttons,
            List<DiscoveredForm> forms
    ) {
        List<LocatorHint> locatorHints = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (DiscoveredInteractiveElement element : links) {
            addLocator(locatorHints, seen, element.locatorHint());
        }
        for (DiscoveredInteractiveElement element : buttons) {
            addLocator(locatorHints, seen, element.locatorHint());
        }
        for (DiscoveredForm form : forms) {
            for (DiscoveredField field : form.fields()) {
                addLocator(locatorHints, seen, field.locatorHint());
            }
            for (DiscoveredInteractiveElement element : form.submitActions()) {
                addLocator(locatorHints, seen, element.locatorHint());
            }
        }

        return locatorHints;
    }

    private void addLocator(List<LocatorHint> locatorHints, Set<String> seen, LocatorHint locatorHint) {
        if (locatorHint == null) {
            return;
        }

        String key = locatorHint.elementName() + "|" + locatorHint.recommendedStrategy() + "|" + locatorHint.recommendedValue();
        if (seen.add(key)) {
            locatorHints.add(locatorHint);
        }
    }

    private String buildFingerprint(String currentUrl, String title, List<String> headings, List<String> capabilities) {
        String raw = currentUrl + "|" + title + "|" + String.join("|", headings) + "|" + String.join("|", capabilities);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private boolean hasPasswordField(DiscoveredForm form) {
        return form.fields().stream().anyMatch(field -> "password".equalsIgnoreCase(field.fieldType()));
    }

    private boolean containsAction(
            List<DiscoveredInteractiveElement> links,
            List<DiscoveredInteractiveElement> buttons,
            String... fragments
    ) {
        return containsAction(links, fragments) || containsAction(buttons, fragments);
    }

    private boolean containsAction(List<DiscoveredInteractiveElement> elements, String... fragments) {
        for (DiscoveredInteractiveElement element : elements) {
            String text = element.visibleText() == null ? "" : element.visibleText().toLowerCase(Locale.ROOT);
            for (String fragment : fragments) {
                if (text.contains(fragment.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int hasVisibleCount(List<DiscoveredInteractiveElement> elements) {
        return (int) elements.stream().filter(DiscoveredInteractiveElement::visible).count();
    }

    private boolean hasStructure(String currentUrl, String pageText, String... fragments) {
        String url = currentUrl.toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            String normalized = fragment.toLowerCase(Locale.ROOT);
            if (url.contains(normalized) || pageText.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean safeDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception exception) {
            return false;
        }
    }

    private String safeText(WebElement element) {
        try {
            String text = element.getText();
            return text == null ? null : text.trim();
        } catch (Exception exception) {
            return null;
        }
    }
}
