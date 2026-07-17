package ua.demo.agentlab.ui.discovery.selenium.auth;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Prevents authentication discovery from typing into a SPA shell before its login form is hydrated.
 * Failure diagnostics stay local to the discovery artifact directory and never include credentials.
 */
public class LoginReadinessPreflight {
    private static final String DOM_SUMMARY_SCRIPT = """
            const visible = e => { const s = getComputedStyle(e); const r = e.getBoundingClientRect();
                return s.display !== 'none' && s.visibility !== 'hidden' && r.width > 0 && r.height > 0; };
            const describe = e => ({tag:e.tagName.toLowerCase(), id:e.id || '', name:e.getAttribute('name') || '',
                type:e.getAttribute('type') || '', role:e.getAttribute('role') || '', text:(e.innerText || '').trim().slice(0,80)});
            return JSON.stringify({readyState:document.readyState,
                visibleInputs:[...document.querySelectorAll('input, textarea, select')].filter(visible).slice(0,20).map(describe),
                visibleButtons:[...document.querySelectorAll('button, input[type=submit], input[type=button]')].filter(visible).slice(0,20).map(describe),
                visibleDialogs:[...document.querySelectorAll('[role=dialog], dialog, [aria-modal=true]')].filter(visible).slice(0,10).map(describe),
                loading:[...document.querySelectorAll('[aria-busy=true], [role=progressbar], [class*=loading i], [class*=spinner i]')].filter(visible).length});
            """;

    private final Path artifactDirectory;

    public LoginReadinessPreflight() {
        this(Path.of("target", "discovery", "preflight"));
    }

    public LoginReadinessPreflight(Path artifactDirectory) {
        this.artifactDirectory = artifactDirectory == null ? Path.of("target", "discovery", "preflight") : artifactDirectory;
    }

    public LoginReadinessPreflightResult await(WebDriver driver, DiscoveryAuthenticationConfig config) {
        if (driver == null || config == null) {
            return LoginReadinessPreflightResult.skipped("driver or authentication configuration is unavailable");
        }
        AtomicReference<Snapshot> latest = new AtomicReference<>(new Snapshot(false, false, false, false,
                "", "", 0, 0, "", List.of()));
        try {
            new WebDriverWait(driver, config.timeout()).until(current -> {
                Snapshot inspected = inspect(current, config);
                latest.set(inspected);
                return inspected.ready();
            });
            return latest.get().toResult("", "");
        } catch (RuntimeException ignored) {
            Snapshot snapshot = inspect(driver, config);
            String reason = snapshot.failureReason();
            String screenshot = captureScreenshot(driver);
            return snapshot.toResult(screenshot, reason);
        }
    }

    private Snapshot inspect(WebDriver driver, DiscoveryAuthenticationConfig config) {
        WebElement username = firstVisible(driver, config.usernameSelector());
        WebElement password = firstVisible(driver, config.passwordSelector());
        WebElement submit = firstDisplayed(driver, config.submitSelector());
        boolean submitEnabled = submit != null && submit.isEnabled();
        String readyState = readReadyState(driver);
        String summary = renderedDomSummary(driver);
        boolean spaReady = "complete".equalsIgnoreCase(readyState)
                && !summary.matches(".*\\\"loading\\\"\\s*:\\s*[1-9].*");
        int inputs = visibleCount(driver, "input, textarea, select");
        int buttons = visibleCount(driver, "button, input[type='submit'], input[type='button']");
        return new Snapshot(username != null, password != null, submitEnabled, spaReady, safeUrl(driver), readyState,
                inputs, buttons, summary, consoleEvents(driver));
    }

    private WebElement firstVisible(WebDriver driver, String selectorList) {
        for (String selector : selectors(selectorList)) {
            try {
                for (WebElement element : driver.findElements(By.cssSelector(selector))) {
                    if (element.isDisplayed() && element.isEnabled()) return element;
                }
            } catch (RuntimeException ignored) {
                // A selector is evidence only; a broken alternative must not abort the preflight.
            }
        }
        return null;
    }

    private WebElement firstDisplayed(WebDriver driver, String selectorList) {
        for (String selector : selectors(selectorList)) {
            try {
                for (WebElement element : driver.findElements(By.cssSelector(selector))) {
                    if (element.isDisplayed()) return element;
                }
            } catch (RuntimeException ignored) {
                // Continue with the remaining profile selectors.
            }
        }
        return null;
    }

    private int visibleCount(WebDriver driver, String selector) {
        try {
            return (int) driver.findElements(By.cssSelector(selector)).stream().filter(WebElement::isDisplayed).count();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private String readReadyState(WebDriver driver) {
        try {
            Object value = ((JavascriptExecutor) driver).executeScript("return document.readyState || '';");
            return value == null ? "" : value.toString().trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String renderedDomSummary(WebDriver driver) {
        try {
            Object value = ((JavascriptExecutor) driver).executeScript(DOM_SUMMARY_SCRIPT);
            return value == null ? "" : value.toString().replaceAll("\\s+", " ").trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private List<String> consoleEvents(WebDriver driver) {
        try {
            List<String> result = new ArrayList<>();
            for (LogEntry entry : driver.manage().logs().get(LogType.BROWSER).getAll()) {
                result.add(entry.getLevel() + " " + entry.getMessage());
                if (result.size() == 20) break;
            }
            return result;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private String captureScreenshot(WebDriver driver) {
        if (!(driver instanceof TakesScreenshot screenshots)) return "";
        try {
            Files.createDirectories(artifactDirectory);
            Path output = artifactDirectory.resolve("login-readiness-" + Instant.now().toEpochMilli() + ".png");
            Files.write(output, screenshots.getScreenshotAs(OutputType.BYTES));
            return output.toAbsolutePath().toString();
        } catch (IOException | RuntimeException ignored) {
            return "";
        }
    }

    private List<String> selectors(String selectorList) {
        if (selectorList == null || selectorList.isBlank()) return List.of();
        return Arrays.stream(selectorList.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList();
    }

    private String safeUrl(WebDriver driver) {
        try { return driver.getCurrentUrl() == null ? "" : driver.getCurrentUrl().trim(); }
        catch (RuntimeException ignored) { return ""; }
    }

    private record Snapshot(boolean usernameVisible, boolean passwordVisible, boolean submitEnabled, boolean spaReady,
                            String currentUrl, String readyState, int visibleInputs, int visibleButtons,
                            String domSummary, List<String> consoleEvents) {
        boolean ready() { return usernameVisible && passwordVisible && submitEnabled && spaReady; }
        String failureReason() {
            List<String> missing = new ArrayList<>();
            if (!usernameVisible) missing.add("username is not visible");
            if (!passwordVisible) missing.add("password is not visible");
            if (!submitEnabled) missing.add("submit is not visible and enabled");
            if (!spaReady) missing.add("SPA is not ready");
            return "login readiness preflight failed: " + String.join("; ", missing);
        }
        LoginReadinessPreflightResult toResult(String screenshot, String reason) {
            return new LoginReadinessPreflightResult(ready(), usernameVisible, passwordVisible, submitEnabled, spaReady,
                    currentUrl, readyState, visibleInputs, visibleButtons, domSummary, consoleEvents, screenshot, reason);
        }
    }
}
