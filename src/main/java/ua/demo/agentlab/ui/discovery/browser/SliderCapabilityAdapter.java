package ua.demo.agentlab.ui.discovery.browser;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SliderCapabilityAdapter implements BrowserCapabilityAdapter {
    private final BrowserLocatorResolver locators = new BrowserLocatorResolver();

    @Override
    public boolean supports(BrowserCapabilityAction action) {
        return action == BrowserCapabilityAction.SET_SLIDER;
    }

    @Override
    public BrowserCapabilityResult execute(WebDriver driver, BrowserCapabilityRequest request) {
        WebElement slider = driver.findElement(locators.resolve(request));
        double target = Double.parseDouble(request.value());
        double min = number(slider.getAttribute("min"), 0.0d);
        double step = Math.max(number(slider.getAttribute("step"), 1.0d), 0.0001d);
        int increments = Math.max(0, (int) Math.round((target - min) / step));
        slider.sendKeys(Keys.HOME);
        for (int index = 0; index < increments; index++) slider.sendKeys(Keys.ARROW_RIGHT);
        double actual = number(slider.getAttribute("value"), Double.NaN);
        return Double.isFinite(actual) && Math.abs(actual - target) <= step / 2.0d
                ? BrowserCapabilityResult.passed("Slider reached the requested value")
                : BrowserCapabilityResult.failed("Slider value does not match the requested value");
    }

    private double number(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (RuntimeException ignored) { return fallback; }
    }
}
