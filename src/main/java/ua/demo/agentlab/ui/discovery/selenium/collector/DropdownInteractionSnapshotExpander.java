package ua.demo.agentlab.ui.discovery.selenium.collector;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.parser.DomParser;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DropdownInteractionSnapshotExpander {

    private static final String TRIGGER_SELECTOR = String.join(", ",
            "[aria-haspopup='true']",
            "[aria-expanded]",
            "[class*='dropdown-tab']",
            "[class*='userdropdown-tab']",
            "[class*='menu-trigger']",
            "[class*='profile-menu']"
    );

    private final DomParser domParser;

    public DropdownInteractionSnapshotExpander() {
        this(new DomParser());
    }

    DropdownInteractionSnapshotExpander(DomParser domParser) {
        this.domParser = domParser == null ? new DomParser() : domParser;
    }

    public List<RawElement> expand(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor executor)) {
            return List.of();
        }
        List<RawElement> expanded = new ArrayList<>();
        List<WebElement> triggers = safeFind(driver, By.cssSelector(TRIGGER_SELECTOR));
        int attempts = 0;
        for (WebElement trigger : triggers) {
            if (attempts >= 3 || !safeDisplayed(trigger) || !safeEnabled(trigger) || hasNavigationHref(trigger)) {
                continue;
            }
            String before = outerHtml(executor);
            safeClick(executor, trigger);
            waitForDomChange(driver, before);
            String after = outerHtml(executor);
            if (!after.isBlank() && !after.equals(before)) {
                expanded.addAll(domParser.parse(snapshot(driver, after)));
                attempts++;
            }
        }
        return expanded;
    }

    private void waitForDomChange(WebDriver driver, String before) {
        if (!(driver instanceof JavascriptExecutor executor)) {
            return;
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(ignored -> {
                String after = outerHtml(executor);
                return !after.isBlank() && !after.equals(before);
            });
        } catch (Exception ignored) {
            // Menus may render synchronously or not mutate; no extra evidence is safer than guessed evidence.
        }
    }

    private RawPageSnapshot snapshot(WebDriver driver, String renderedDom) {
        return new RawPageSnapshot(
                safe(driver.getCurrentUrl()),
                "",
                safe(driver.getTitle()),
                renderedDom,
                renderedDom,
                "",
                "",
                "",
                Instant.now().toString(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private List<WebElement> safeFind(WebDriver driver, By by) {
        try {
            return driver.findElements(by);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void safeClick(JavascriptExecutor executor, WebElement element) {
        try {
            executor.executeScript("arguments[0].click();", element);
        } catch (Exception ignored) {
            try {
                element.click();
            } catch (Exception ignoredAgain) {
                // Ignore non-actionable dropdown candidates.
            }
        }
    }

    private String outerHtml(JavascriptExecutor executor) {
        try {
            Object result = executor.executeScript("return document.documentElement.outerHTML;");
            return result == null ? "" : String.valueOf(result);
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean safeDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean safeEnabled(WebElement element) {
        try {
            return element.isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasNavigationHref(WebElement element) {
        try {
            String href = element.getAttribute("href");
            return href != null && !href.isBlank();
        } catch (Exception ignored) {
            return false;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
