package ua.demo.agentlab.ui.discovery.selenium.extractor;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredInteractiveElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InteractiveElementExtractor {

    public List<DiscoveredInteractiveElement> extractLinks(WebDriver driver) {
        List<DiscoveredInteractiveElement> links = new ArrayList<>(
                extractElements(driver.findElements(By.cssSelector("a[href]")), "link", false)
        );
        // SPA sidebars are often collapsed in headless discovery. Retain their requirement-matched
        // routes as non-visible navigation evidence; the crawler still validates the destination.
        links.addAll(extractElements(driver.findElements(By.cssSelector(
                "nav a[href], [role='navigation'] a[href], aside a[href]"
        )), "link", true));
        return deduplicate(links);
    }

    public List<DiscoveredInteractiveElement> extractButtons(WebDriver driver) {
        List<WebElement> elements = new ArrayList<>();
        elements.addAll(driver.findElements(By.cssSelector("button")));
        elements.addAll(driver.findElements(By.cssSelector("input[type='submit'], input[type='button']")));
        elements.addAll(driver.findElements(By.cssSelector("[role='button']")));
        return extractElements(elements, "button", false);
    }

    public List<DiscoveredInteractiveElement> extractSubmitActions(WebElement form) {
        List<WebElement> elements = new ArrayList<>();
        elements.addAll(form.findElements(By.cssSelector("button[type='submit'], input[type='submit']")));
        return extractElements(elements, "submit", false);
    }

    private List<DiscoveredInteractiveElement> extractElements(
            List<WebElement> elements,
            String elementType,
            boolean includeHiddenNavigation
    ) {
        List<DiscoveredInteractiveElement> discovered = new ArrayList<>();

        for (WebElement element : elements) {
            boolean displayed = safeDisplayed(element);
            if (!displayed && !includeHiddenNavigation) {
                continue;
            }

            String visibleText = firstNonBlank(
                    safeText(element),
                    safeAttribute(element, "value"),
                    safeAttribute(element, "aria-label"),
                    safeAttribute(element, "name"),
                    safeAttribute(element, "id")
            );
            String role = safeAttribute(element, "role");
            String name = safeAttribute(element, "name");
            String id = safeAttribute(element, "id");
            String href = firstNonBlank(
                    safeAttribute(element, "href"),
                    safeAttribute(element, "data-href")
            );

            discovered.add(new DiscoveredInteractiveElement(
                    elementType,
                    visibleText,
                    role,
                    name,
                    id,
                    href,
                    safeEnabled(element),
                    displayed,
                    toLocatorHint(elementType, visibleText, name, id)
            ));
        }

        return discovered;
    }

    private List<DiscoveredInteractiveElement> deduplicate(List<DiscoveredInteractiveElement> elements) {
        Map<String, DiscoveredInteractiveElement> unique = new LinkedHashMap<>();
        for (DiscoveredInteractiveElement element : elements) {
            String key = safe(element.href()) + "|" + safe(element.visibleText()) + "|" + safe(element.id());
            DiscoveredInteractiveElement existing = unique.get(key);
            if (existing == null || (!existing.visible() && element.visible())) {
                unique.put(key, element);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private LocatorHint toLocatorHint(String elementType, String visibleText, String name, String id) {
        String elementName = firstNonBlank(visibleText, name, id, elementType);

        if (id != null && !id.isBlank()) {
            return new LocatorHint(elementName, "id", id);
        }
        if (name != null && !name.isBlank()) {
            return new LocatorHint(elementName, "name", name);
        }
        if (visibleText != null && !visibleText.isBlank()) {
            return new LocatorHint(elementName, "partialLinkText", visibleText);
        }

        return new LocatorHint(elementName, "css", elementType);
    }

    private boolean safeDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean safeEnabled(WebElement element) {
        try {
            return element.isEnabled();
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

    private String safeAttribute(WebElement element, String attribute) {
        try {
            String value = element.getAttribute(attribute);
            return value == null ? null : value.trim();
        } catch (Exception exception) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
