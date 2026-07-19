package ua.demo.agentlab.ui.discovery.browser;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Files;
import java.nio.file.Path;

public class UploadCapabilityAdapter implements BrowserCapabilityAdapter {
    private final BrowserLocatorResolver locators = new BrowserLocatorResolver();

    @Override
    public boolean supports(BrowserCapabilityAction action) {
        return action == BrowserCapabilityAction.UPLOAD_FILE;
    }

    @Override
    public BrowserCapabilityResult execute(WebDriver driver, BrowserCapabilityRequest request) {
        Path file = Path.of(request.value()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) return BrowserCapabilityResult.failed("Upload file does not exist");
        WebElement input = driver.findElement(locators.resolve(request));
        input.sendKeys(file.toString());
        String value = input.getDomProperty("value");
        return value == null || value.isBlank()
                ? BrowserCapabilityResult.failed("File input did not retain a selected file")
                : BrowserCapabilityResult.passed("File input accepted the configured file");
    }
}
