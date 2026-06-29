package ua.demo.agentlab.core.ui.locators;

import ua.demo.agentlab.ui.LocatorHint;

public class SeleniumLocatorMapper {

    public String toByExpression(LocatorHint locatorHint) {
        String strategy = locatorHint.recommendedStrategy();
        String value = locatorHint.recommendedValue();

        return switch (strategy) {
            case "id" -> "By.id(\"" + escapeJava(value) + "\")";
            case "name" -> "By.name(\"" + escapeJava(value) + "\")";
            case "css", "cssSelector" -> "By.cssSelector(\"" + escapeJava(value) + "\")";
            case "xpath" -> "By.xpath(\"" + escapeJava(value) + "\")";
            case "text" -> "By.xpath(\"//*[normalize-space()='" + escapeXPath(value) + "']\")";
            case "linkText" -> "By.linkText(\"" + escapeJava(value) + "\")";
            case "partialLinkText" -> "By.partialLinkText(\"" + escapeJava(value) + "\")";
            case "className" -> "By.className(\"" + escapeJava(value) + "\")";
            case "tagName" -> "By.tagName(\"" + escapeJava(value) + "\")";
            case "getByTestId" -> "By.cssSelector(\"[data-testid='" + escapeCss(value) + "']\")";
            case "getByLabel" -> "By.xpath(\"//label[normalize-space()='" + escapeXPath(value)
                    + "']/following::input[1]\")";
            case "getByRole" -> mapRoleLocator(value);
            default -> throw new IllegalStateException(
                    "Unsupported locator strategy for Selenium: " + locatorHint.recommendedStrategy()
            );
        };
    }

    private String mapRoleLocator(String value) {
        if (value.startsWith("button[")) {
            String name = extractRoleName(value);
            return "By.xpath(\"//button[normalize-space()='" + escapeXPath(name) + "']\")";
        }

        if (value.startsWith("link[")) {
            String name = extractRoleName(value);
            return "By.xpath(\"//a[normalize-space()='" + escapeXPath(name) + "']\")";
        }

        throw new IllegalStateException("Unsupported role locator format: " + value);
    }

    private String extractRoleName(String value) {
        int start = value.indexOf("name='");
        int end = value.lastIndexOf("'");
        if (start < 0 || end <= start + 6) {
            throw new IllegalStateException("Cannot parse role locator name: " + value);
        }
        return value.substring(start + 6, end);
    }

    private String escapeCss(String value) {
        return value.replace("'", "\\'");
    }

    private String escapeXPath(String value) {
        return value.replace("'", "\\'");
    }

    private String escapeJava(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
