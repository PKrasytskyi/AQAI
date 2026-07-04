package ua.demo.agentlab.ui.discovery.selenium.readiness;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PageReadinessWaiter {

    public void waitUntilReady(WebDriver driver, PageReadinessRule rule) {
        if (driver == null || !(driver instanceof JavascriptExecutor)) {
            return;
        }
        PageReadinessRule resolvedRule = rule == null ? PageReadinessRule.generic("", 8_000L) : rule;
        try {
            new WebDriverWait(driver, Duration.ofMillis(resolvedRule.timeoutMillis()))
                    .until(currentDriver -> Boolean.TRUE.equals(isReady(currentDriver, resolvedRule)));
        } catch (RuntimeException ignored) {
            // Discovery keeps the best available evidence when the application never reaches the configured readiness state.
        }
    }

    private Boolean isReady(WebDriver driver, PageReadinessRule rule) {
        if (!(driver instanceof JavascriptExecutor javascriptExecutor)) {
            return true;
        }
        try {
            Object result = javascriptExecutor.executeScript("""
                    const requiredSelectors = Array.from(arguments[0] || []);
                    const readyTextFragments = Array.from(arguments[1] || []).map(v => String(v).toLowerCase());
                    const ready = document.readyState === 'complete' || document.readyState === 'interactive';
                    const bodyText = (document.body && document.body.innerText || '').trim();
                    const normalizedText = bodyText.toLowerCase();
                    const controls = document.querySelectorAll("input:not([type='hidden']), button, select, textarea, [role='button']").length;
                    const samePageLinks = document.querySelectorAll("a[href]").length;

                    function visible(element) {
                      if (!element) return false;
                      const style = window.getComputedStyle(element);
                      const rect = element.getBoundingClientRect();
                      return style.visibility !== 'hidden'
                        && style.display !== 'none'
                        && rect.width > 0
                        && rect.height > 0;
                    }

                    function selectorReady(selector) {
                      try {
                        return Array.from(document.querySelectorAll(selector)).some(visible);
                      } catch (e) {
                        return false;
                      }
                    }

                    const requiredSelectorsReady = requiredSelectors.length === 0
                      || requiredSelectors.every(selectorReady);
                    const textReady = readyTextFragments.length === 0
                      || readyTextFragments.some(fragment => normalizedText.includes(fragment));
                    const genericReady = controls > 0 || bodyText.length > 0 || samePageLinks > 1;

                    return ready && (requiredSelectors.length > 0
                      ? requiredSelectorsReady
                      : (textReady || genericReady));
                    """, rule.requiredCssSelectors(), rule.readyTextFragments());
            return Boolean.TRUE.equals(result);
        } catch (RuntimeException exception) {
            return true;
        }
    }
}
