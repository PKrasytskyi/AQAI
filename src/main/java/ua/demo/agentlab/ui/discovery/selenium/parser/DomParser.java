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
            "[aria-haspopup]",
            "[aria-expanded]",
            "[role='checkbox']",
            "[role='radio']",
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

    private boolean isVisible(Element element) {
        String style = element.attr("style").toLowerCase(java.util.Locale.ROOT);
        String type = element.attr("type").toLowerCase(java.util.Locale.ROOT);
        if ("hidden".equals(type) || element.hasAttr("hidden")) {
            return false;
        }
        if ("true".equalsIgnoreCase(element.attr("aria-hidden"))) {
            return false;
        }
        return !style.contains("display:none")
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
