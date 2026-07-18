package ua.demo.agentlab.ui.discovery.selenium.extractor;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredField;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredForm;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FormStructureExtractor {

    private final InteractiveElementExtractor interactiveElementExtractor;

    public FormStructureExtractor(InteractiveElementExtractor interactiveElementExtractor) {
        if (interactiveElementExtractor == null) {
            throw new IllegalArgumentException("interactiveElementExtractor cannot be null");
        }
        this.interactiveElementExtractor = interactiveElementExtractor;
    }

    public List<DiscoveredForm> extractForms(WebDriver driver) {
        List<DiscoveredForm> forms = new ArrayList<>();

        for (WebElement form : driver.findElements(By.cssSelector("form"))) {
            if (!safeDisplayed(form)) {
                continue;
            }

            List<DiscoveredField> fields = extractFields(form);
            forms.add(new DiscoveredForm(
                    safeAttribute(form, "id"),
                    safeAttribute(form, "name"),
                    safeAttribute(form, "action"),
                    fields,
                    interactiveElementExtractor.extractSubmitActions(form)
            ));
        }

        return forms;
    }

    private List<DiscoveredField> extractFields(WebElement form) {
        List<DiscoveredField> fields = new ArrayList<>();
        Set<WebElement> elements = new LinkedHashSet<>(form.findElements(By.cssSelector(
                "input:not([type='hidden']):not([type='submit']):not([type='button']), select, textarea, "
                        + "[role='combobox'], [role='listbox'], [class*='select'][tabindex], "
                        + "[class*='select'] [tabindex]:not(input):not(button):not(a)"
        )));

        for (WebElement field : elements) {
            if (!safeDisplayed(field)) {
                continue;
            }

            String id = safeAttribute(field, "id");
            String name = safeAttribute(field, "name");
            String label = firstNonBlank(
                    safeAttribute(field, "aria-label"),
                    safeAttribute(field, "placeholder"),
                    nearestLabel(field),
                    name,
                    id
            );

            fields.add(new DiscoveredField(
                    detectFieldType(field),
                    name,
                    id,
                    label,
                    safeRequired(field),
                    safeAttribute(field, "placeholder"),
                    toLocatorHint(label, name, id)
            ));
        }

        return fields;
    }

    private String detectFieldType(WebElement field) {
        String tag = field.getTagName();
        if ("select".equalsIgnoreCase(tag)) {
            return "select";
        }
        if ("textarea".equalsIgnoreCase(tag)) {
            return "textarea";
        }
        String role = safeAttribute(field, "role");
        String cssClass = safeAttribute(field, "class");
        if ("combobox".equalsIgnoreCase(role)
                || "listbox".equalsIgnoreCase(role)
                || (cssClass != null && cssClass.toLowerCase().contains("select"))) {
            return "select";
        }

        String inputType = safeAttribute(field, "type");
        return (inputType == null || inputType.isBlank()) ? "text" : inputType;
    }

    private LocatorHint toLocatorHint(String label, String name, String id) {
        String elementName = firstNonBlank(label, name, id, "field");
        if (id != null && !id.isBlank()) {
            return new LocatorHint(elementName, "id", id);
        }
        if (name != null && !name.isBlank()) {
            return new LocatorHint(elementName, "name", name);
        }
        if (label != null && !label.isBlank()) {
            String escaped = label.replace("'", "\\'");
            return new LocatorHint(
                    elementName,
                    "xpath",
                    "(//label[normalize-space()='" + escaped
                            + "']/ancestor::*[.//*[@tabindex]][1]//*[@tabindex][1])"
            );
        }
        return new LocatorHint(elementName, "css", "input, select, textarea");
    }

    private String nearestLabel(WebElement field) {
        try {
            List<WebElement> labels = field.findElements(By.xpath(
                    "ancestor::*[.//label and .//*[@tabindex]][1]//label[1]"
            ));
            if (!labels.isEmpty()) {
                String text = labels.get(0).getText();
                return text == null ? null : text.trim();
            }
        } catch (Exception ignored) {
            // Missing label ancestry is valid for controls labelled through ARIA or placeholder.
        }
        return null;
    }

    private boolean safeDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean safeRequired(WebElement element) {
        String required = safeAttribute(element, "required");
        String ariaRequired = safeAttribute(element, "aria-required");
        return required != null || "true".equalsIgnoreCase(ariaRequired);
    }

    private String safeAttribute(WebElement element, String attribute) {
        try {
            String value = element.getDomAttribute(attribute);
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
}
