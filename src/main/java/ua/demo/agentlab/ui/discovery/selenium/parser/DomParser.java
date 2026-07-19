package ua.demo.agentlab.ui.discovery.selenium.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DomParser {

    private static final String IMPORTANT_SELECTOR = String.join(", ",
            "input",
            "button",
            "a[href]",
            "select",
            "textarea",
            "label",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "form",
            "table",
            "tr",
            "td",
            "th",
            "dialog",
            "[role='dialog']",
            "[role='button']",
            "[role='combobox']",
            "[role='listbox']",
            "[role='option']",
            "[aria-haspopup]",
            "[aria-expanded]",
            "form [class*='select'] [tabindex]:not([tabindex='-1'])",
            "[role='checkbox']",
            "[role='radio']",
            "[role='table']",
            "[role='grid']",
            "[role='rowgroup']",
            "[role='row']",
            "[role='cell']",
            "[role='columnheader']",
            "[type='checkbox']",
            "[type='radio']",
            "[type='file']",
            "[aria-live]",
            "[role='alert']",
            ".modal",
            ".toast",
            ".error",
            ".success",
            ".alert",
            "span[class*=dropdown]",
            "div[class*=dropdown]",
            "span[class*=menu]",
            "div[class*=menu]"
    );

    public List<RawElement> parse(RawPageSnapshot rawPageSnapshot) {
        if (rawPageSnapshot == null || rawPageSnapshot.renderedDom().isBlank()) {
            return List.of();
        }
        Document document = Jsoup.parse(rawPageSnapshot.renderedDom(), rawPageSnapshot.currentUrl());
        List<RawElement> elements = new ArrayList<>();
        int index = 0;
        for (Element element : document.select(IMPORTANT_SELECTOR)) {
            elements.add(toRawElement(element, index++));
        }
        return elements;
    }

    public List<RawElement> parse(WebDriver driver) {
        if (driver == null) {
            return List.of();
        }
        try {
            return parse(new RawPageSnapshot(
                    driver.getCurrentUrl(),
                    "",
                    driver.getTitle(),
                    driver.getPageSource(),
                    driver.getPageSource(),
                    "",
                    "",
                    "",
                    "",
                    List.of(),
                    Map.of(),
                    Map.of(),
                    List.of(),
                    List.of(),
                    List.of()
            ));
        } catch (Exception exception) {
            return List.of();
        }
    }

    private RawElement toRawElement(Element element, int index) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (Attribute attribute : element.attributes()) {
            attributes.put(attribute.getKey(), attribute.getValue());
        }
        addContainerContext(element, attributes);
        addFieldContext(element, attributes);
        return new RawElement(
                "raw-" + index,
                element.tagName(),
                element.attr("type"),
                element.text(),
                element.id(),
                element.attr("name"),
                element.attr("placeholder"),
                element.attr("aria-label"),
                element.attr("role"),
                element.attr("href"),
                firstNonBlank(element.attr("data-testid"), element.attr("data-test"), element.attr("data-qa")),
                element.className(),
                isVisible(element),
                isEnabled(element),
                isRequired(element),
                attributes
        );
    }

    /**
     * Jsoup preserves rendered DOM ancestry. Store a compact nearest landmark so component
     * grouping can use an actual container before falling back to keyword heuristics.
     */
    private void addContainerContext(Element element, Map<String, String> attributes) {
        Element container = element.closest("form, header, nav, aside, main, section, dialog, table, [role=menu], [role=navigation], [role=dialog], [role=table], [role=grid], [data-testid], [data-test], [data-qa]");
        if (container == null) {
            return;
        }
        String tag = container.tagName();
        String id = container.id();
        String role = container.attr("role");
        String dataId = firstNonBlank(container.attr("data-testid"), container.attr("data-test"), container.attr("data-qa"));
        String key = !dataId.isBlank() ? "data=" + dataId
                : !id.isBlank() ? "id=" + id
                : !role.isBlank() ? "role=" + role + ":tag=" + tag
                : "tag=" + tag;
        attributes.put("agentlab.container.key", key);
        attributes.put("agentlab.container.tag", tag);
        attributes.put("agentlab.container.id", id);
        attributes.put("agentlab.container.role", role);
        attributes.put("agentlab.container.data-testid", dataId);
        attributes.put("agentlab.dom.ancestry", ancestry(element));
    }

    private void addFieldContext(Element element, Map<String, String> attributes) {
        if (!isCustomFormControl(element)) {
            return;
        }
        Element group = element.parent();
        Element label = null;
        for (int depth = 0; group != null && depth < 7; depth++, group = group.parent()) {
            label = group.selectFirst("label");
            if (label != null) {
                break;
            }
            if ("form".equalsIgnoreCase(group.tagName())) {
                break;
            }
        }
        String labelText = label == null ? "" : label.text().trim();
        if (labelText.isBlank()) {
            return;
        }
        attributes.put("agentlab.field.kind", "custom-select");
        attributes.put("agentlab.field.label", labelText);
        attributes.put("agentlab.field.locator.xpath", customControlXpath(labelText));
    }

    private boolean isCustomFormControl(Element element) {
        String role = element.attr("role").toLowerCase(java.util.Locale.ROOT);
        String popup = element.attr("aria-haspopup").toLowerCase(java.util.Locale.ROOT);
        String classes = element.className().toLowerCase(java.util.Locale.ROOT);
        boolean focusableSemanticControl = element.hasAttr("tabindex")
                && !"-1".equals(element.attr("tabindex"))
                && (classes.contains("select") || classes.contains("autocomplete") || classes.contains("combobox"));
        return "combobox".equals(role) || "listbox".equals(popup) || focusableSemanticControl;
    }

    private String customControlXpath(String label) {
        String control = "*[@role='combobox' or @aria-haspopup='listbox' or "
                + "(@tabindex and not(@tabindex='-1') and "
                + "(contains(translate(@class,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'select') "
                + "or contains(translate(@class,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'autocomplete') "
                + "or contains(translate(@class,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'combobox')))]";
        return "(//label[normalize-space()=" + xpathLiteral(label) + "]"
                + "/ancestor::*[.//" + control + "][1]//" + control + "[1])";
    }

    private String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        String[] parts = value.split("'", -1);
        List<String> literals = new ArrayList<>();
        for (int index = 0; index < parts.length; index++) {
            if (!parts[index].isEmpty()) {
                literals.add("'" + parts[index] + "'");
            }
            if (index < parts.length - 1) {
                literals.add("\"'\"");
            }
        }
        return "concat(" + String.join(",", literals) + ")";
    }

    private String ancestry(Element element) {
        List<String> parts = new ArrayList<>();
        Element current = element;
        for (int depth = 0; current != null && depth < 6; depth++, current = current.parent()) {
            String part = current.tagName();
            if (!current.id().isBlank()) part += "#" + current.id();
            String role = current.attr("role");
            if (!role.isBlank()) part += "[role=" + role + "]";
            parts.add(part);
        }
        return String.join(" > ", parts);
    }

    private boolean isVisible(Element element) {
        String style = element.attr("style").toLowerCase(java.util.Locale.ROOT);
        String type = element.attr("type").toLowerCase(java.util.Locale.ROOT);
        String cssClass = element.className().toLowerCase(java.util.Locale.ROOT);
        if ("hidden".equals(type) || element.hasAttr("hidden")) {
            return false;
        }
        if ("true".equalsIgnoreCase(element.attr("aria-hidden"))) {
            return false;
        }
        return !cssClass.matches(".*(?:^|\\s)(?:hidden|hide|[^\\s]+--hide)(?:\\s|$).*")
                && !style.contains("display:none")
                && !style.contains("display: none")
                && !style.contains("visibility:hidden")
                && !style.contains("visibility: hidden");
    }

    private boolean isEnabled(Element element) {
        return !element.hasAttr("disabled")
                && !"true".equalsIgnoreCase(element.attr("aria-disabled"));
    }

    private boolean isRequired(Element element) {
        return element.hasAttr("required")
                || "true".equalsIgnoreCase(element.attr("aria-required"));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
