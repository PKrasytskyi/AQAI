package ua.demo.agentlab.ui.discovery.mapping;

import java.util.Locale;

public enum LocatorStrategy {
    CSS("css"),
    ID("id"),
    NAME("name"),
    XPATH("xpath"),
    LINK_TEXT("linkText"),
    PARTIAL_LINK_TEXT("partialLinkText"),
    TAG_NAME("tagName"),
    CLASS_NAME("className"),
    UNKNOWN("unknown");

    private final String wireName;

    LocatorStrategy(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static LocatorStrategy from(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return switch (normalized) {
            case "css", "cssselector" -> CSS;
            case "id" -> ID;
            case "name" -> NAME;
            case "xpath" -> XPATH;
            case "linktext" -> LINK_TEXT;
            case "partiallinktext" -> PARTIAL_LINK_TEXT;
            case "tagname" -> TAG_NAME;
            case "classname" -> CLASS_NAME;
            default -> UNKNOWN;
        };
    }

    public boolean isBlank() {
        return this == UNKNOWN;
    }

    @Override
    public String toString() {
        return wireName;
    }
}
