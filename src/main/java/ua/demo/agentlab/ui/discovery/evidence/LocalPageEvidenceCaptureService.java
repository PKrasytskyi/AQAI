package ua.demo.agentlab.ui.discovery.evidence;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.ui.discovery.evidence.model.DiscoveredPageEvidence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalPageEvidenceCaptureService implements PageEvidenceCaptureService {

    private final Path outputDirectory;

    public LocalPageEvidenceCaptureService() {
        this(Path.of("target", "discovery", "pages"));
    }

    public LocalPageEvidenceCaptureService(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory cannot be null");
        }
        this.outputDirectory = outputDirectory;
    }

    @Override
    public DiscoveredPageEvidence capture(WebDriver driver, String pageId) {
        try {
            Files.createDirectories(outputDirectory);

            String normalizedPageId = normalizePageId(pageId);
            Path screenshotPath = outputDirectory.resolve(normalizedPageId + ".png");
            Path htmlPath = outputDirectory.resolve(normalizedPageId + ".html");

            writeScreenshot(driver, screenshotPath);
            writeHtml(driver, htmlPath);

            return new DiscoveredPageEvidence(
                    screenshotPath.toString(),
                    htmlPath.toString()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot capture page evidence for: " + pageId, exception);
        }
    }

    private void writeScreenshot(WebDriver driver, Path outputFile) throws IOException {
        if (driver instanceof TakesScreenshot takesScreenshot) {
            byte[] bytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);
            Files.write(outputFile, bytes);
        }
    }

    private void writeHtml(WebDriver driver, Path outputFile) throws IOException {
        String html = "";
        if (driver instanceof JavascriptExecutor javascriptExecutor) {
            try {
                Object renderedDom = javascriptExecutor.executeScript("return document.documentElement.outerHTML;");
                html = renderedDom == null ? "" : String.valueOf(renderedDom);
            } catch (Exception ignored) {
                html = "";
            }
        }
        if (html.isBlank()) {
            String pageSource = driver.getPageSource();
            html = pageSource == null ? "" : pageSource;
        }
        Files.writeString(outputFile, html);
    }

    private String normalizePageId(String pageId) {
        if (pageId == null || pageId.isBlank()) {
            return "page";
        }

        String normalized = pageId.trim()
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "page" : normalized.toLowerCase();
    }
}
