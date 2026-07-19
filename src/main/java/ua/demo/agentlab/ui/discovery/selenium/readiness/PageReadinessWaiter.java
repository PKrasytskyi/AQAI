package ua.demo.agentlab.ui.discovery.selenium.readiness;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PageReadinessWaiter {

    public PageReadinessResult waitUntilReady(WebDriver driver, PageReadinessRule rule) {
        PageReadinessRule resolvedRule = rule == null ? PageReadinessRule.generic("", 8_000L) : rule;
        if (driver == null || !(driver instanceof JavascriptExecutor)) {
            return PageReadinessResult.degraded(resolvedRule, "JavaScript-capable WebDriver is unavailable.");
        }
        try {
            new WebDriverWait(driver, Duration.ofMillis(resolvedRule.timeoutMillis()))
                    .until(currentDriver -> Boolean.TRUE.equals(isReady(currentDriver, resolvedRule)));
            return PageReadinessResult.ready(resolvedRule);
        } catch (RuntimeException ignored) {
            return PageReadinessResult.degraded(resolvedRule,
                    "Readiness timeout or browser error after " + resolvedRule.timeoutMillis() + " ms: "
                            + concise(ignored));
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
                    const textReady = readyTextFragments.length > 0
                      && readyTextFragments.some(fragment => normalizedText.includes(fragment));
                    const structural = Array.from(document.querySelectorAll(
                      "main, nav, form, table, [role='main'], [role='navigation'], [role='table'], [role='grid']"
                    )).filter(visible).length;
                    const genericReady = controls > 0 || samePageLinks > 1 || structural > 0 || bodyText.length >= 24;
                    const candidateReady = ready && (requiredSelectors.length > 0
                      ? requiredSelectorsReady
                      : (textReady || genericReady));
                    if (!candidateReady) {
                      window.__agentlabReadinessProbe = null;
                      return false;
                    }

                    const fingerprint = [location.href, document.readyState, bodyText.length,
                      normalizedText.substring(0, 160), controls, samePageLinks, structural,
                      document.querySelectorAll('*').length].join('|');
                    const now = Date.now();
                    const previous = window.__agentlabReadinessProbe;
                    if (!previous || previous.fingerprint !== fingerprint) {
                      window.__agentlabReadinessProbe = {fingerprint, since: now};
                      return false;
                    }
                    return now - previous.since >= 300;
                    """, rule.requiredCssSelectors(), rule.readyTextFragments());
            return Boolean.TRUE.equals(result);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String concise(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message.replaceAll("\\s+", " ").trim();
    }
}
