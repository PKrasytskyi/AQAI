package ua.demo.agentlab.ui.discovery.selenium.model;

import org.openqa.selenium.Cookie;

import java.util.List;
import java.util.Map;

public record RawPageSnapshot(
        String currentUrl,
        String route,
        String title,
        String pageSource,
        String renderedDom,
        String visibleText,
        String screenshotPath,
        String htmlPath,
        String timestamp,
        List<Cookie> cookies,
        Map<String, String> localStorage,
        Map<String, String> sessionStorage,
        List<BrowserLogEntry> browserConsoleLogs,
        List<BrowserLogEntry> performanceLogs,
        List<BrowserNetworkCall> networkCalls
) {
    public RawPageSnapshot {
        currentUrl = currentUrl == null ? "" : currentUrl.trim();
        route = route == null ? "" : route.trim();
        title = title == null ? "" : title.trim();
        pageSource = pageSource == null ? "" : pageSource;
        renderedDom = renderedDom == null ? "" : renderedDom;
        visibleText = visibleText == null ? "" : visibleText.trim();
        screenshotPath = screenshotPath == null ? "" : screenshotPath.trim();
        htmlPath = htmlPath == null ? "" : htmlPath.trim();
        timestamp = timestamp == null ? "" : timestamp.trim();
        cookies = cookies == null ? List.of() : List.copyOf(cookies);
        localStorage = localStorage == null ? Map.of() : Map.copyOf(localStorage);
        sessionStorage = sessionStorage == null ? Map.of() : Map.copyOf(sessionStorage);
        browserConsoleLogs = browserConsoleLogs == null ? List.of() : List.copyOf(browserConsoleLogs);
        performanceLogs = performanceLogs == null ? List.of() : List.copyOf(performanceLogs);
        networkCalls = networkCalls == null ? List.of() : List.copyOf(networkCalls);
    }
}
