package ua.demo.agentlab.ui.discovery.selenium.extractor;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredField;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredForm;

import java.util.ArrayList;
import java.util.List;

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
        List<WebElement> elements = form.findElements(By.cssSelector(
                "input:not([type='hidden']):not([type='submit']):not([type='button']), select, textarea"
        ));

        for (WebElement field : elements) {
            if (!safeDisplayed(field)) {
                continue;
            }

            String id = safeAttribute(field, "id");
            String name = safeAttribute(field, "name");
            String label = firstNonBlank(
                    safeAttribute(field, "aria-label"),
                    safeAttribute(field, "placeholder"),
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
        return new LocatorHint(elementName, "css", "input, select, textarea");
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
}
