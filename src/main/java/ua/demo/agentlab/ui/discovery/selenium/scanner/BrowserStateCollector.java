package ua.demo.agentlab.ui.discovery.selenium.scanner;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.LinkedHashMap;
import java.util.Map;

public class BrowserStateCollector {

    public Map<String, String> collectLocalStorage(WebDriver driver) {
        return collectStorage(driver, "localStorage");
    }

    public Map<String, String> collectSessionStorage(WebDriver driver) {
        return collectStorage(driver, "sessionStorage");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> collectStorage(WebDriver driver, String storageName) {
        if (!(driver instanceof JavascriptExecutor javascriptExecutor)) {
            return Map.of();
        }
        try {
            Object result = javascriptExecutor.executeScript("""
                    const storage = window[arguments[0]];
                    const values = {};
                    for (let i = 0; i < storage.length; i++) {
                      const key = storage.key(i);
                      values[key] = storage.getItem(key);
                    }
                    return values;
                    """, storageName);
            if (!(result instanceof Map<?, ?> rawMap)) {
                return Map.of();
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    values.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
            return values;
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
