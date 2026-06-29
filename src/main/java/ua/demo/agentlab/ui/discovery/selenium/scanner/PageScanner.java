package ua.demo.agentlab.ui.discovery.selenium.scanner;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.ui.discovery.evidence.model.DiscoveredPageEvidence;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserLogEntry;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class PageScanner {

    private final BrowserStateCollector browserStateCollector;
    private final NetworkLogCollector networkLogCollector;

    public PageScanner() {
        this(new BrowserStateCollector(), new NetworkLogCollector());
    }

    public PageScanner(BrowserStateCollector browserStateCollector, NetworkLogCollector networkLogCollector) {
        if (browserStateCollector == null) {
            throw new IllegalArgumentException("browserStateCollector cannot be null");
        }
        if (networkLogCollector == null) {
            throw new IllegalArgumentException("networkLogCollector cannot be null");
        }
        this.browserStateCollector = browserStateCollector;
        this.networkLogCollector = networkLogCollector;
    }

    public RawPageSnapshot scan(WebDriver driver, DiscoveredPageEvidence evidence) {
        String currentUrl = safe(driver.getCurrentUrl());
        String title = safe(driver.getTitle());
        String pageSource = safe(driver.getPageSource());
        String renderedDom = executeStringScript(driver, "return document.documentElement.outerHTML;");
        String visibleText = executeStringScript(driver, "return document.body ? document.body.innerText : '';");
        List<BrowserLogEntry> performanceLogs = networkLogCollector.collectPerformanceLogs(driver);

        return new RawPageSnapshot(
                currentUrl,
                normalizeRoute(currentUrl),
                title,
                pageSource,
                renderedDom,
                visibleText,
                evidence == null ? "" : evidence.screenshotPath(),
                evidence == null ? "" : evidence.htmlPath(),
                Instant.now().toString(),
                driver.manage().getCookies().stream().toList(),
                browserStateCollector.collectLocalStorage(driver),
                browserStateCollector.collectSessionStorage(driver),
                networkLogCollector.collectBrowserLogs(driver),
                performanceLogs,
                networkLogCollector.extractNetworkCalls(performanceLogs)
        );
    }

    private String executeStringScript(WebDriver driver, String script) {
        if (!(driver instanceof JavascriptExecutor javascriptExecutor)) {
            return "";
        }
        try {
            Object result = javascriptExecutor.executeScript(script);
            return result == null ? "" : String.valueOf(result);
        } catch (Exception exception) {
            return "";
        }
    }

    private String normalizeRoute(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            return path == null || path.isBlank() ? "/" : path;
        } catch (Exception exception) {
            return url == null || url.isBlank() ? "/" : url;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
