package ua.demo.agentlab.ui.discovery.runtime.bidi;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SeleniumBiDiSessionAdapter {

    private final BiDiEventBuffer eventBuffer;
    private String currentPageId = "";
    private String currentPageUrl = "";
    private boolean started;

    public SeleniumBiDiSessionAdapter(BiDiEventBuffer eventBuffer) {
        this.eventBuffer = eventBuffer;
    }

    public boolean isAvailable() {
        return eventBuffer != null;
    }

    public void start(WebDriver driver, String pageId, String pageUrl) {
        if (!isAvailable() || !(driver instanceof JavascriptExecutor)) {
            return;
        }
        this.currentPageId = clean(pageId);
        this.currentPageUrl = clean(pageUrl);
        this.started = true;
        addEvent("bidi.session.start", Map.of(
                "url", this.currentPageUrl,
                "pageUrl", this.currentPageUrl,
                "trigger", "discovery-session"
        ));
        installInstrumentation(driver);
        drain(driver);
    }

    public void drain(WebDriver driver) {
        if (!started || !isAvailable() || !(driver instanceof JavascriptExecutor javascriptExecutor)) {
            return;
        }
        Object rawEvents;
        try {
            rawEvents = javascriptExecutor.executeScript(
                    "return (window.__agentLabBiDiDrain && window.__agentLabBiDiDrain()) || [];"
            );
        } catch (RuntimeException exception) {
            addEvent("log.entryAdded", Map.of(
                    "level", "WARN",
                    "message", "Failed to drain browser-side BiDi events: " + exception.getMessage(),
                    "pageUrl", currentPageUrl
            ));
            return;
        }
        if (!(rawEvents instanceof List<?> events)) {
            return;
        }
        for (Object rawEvent : events) {
            if (!(rawEvent instanceof Map<?, ?> eventMap)) {
                continue;
            }
            Map<String, String> attributes = toAttributes(eventMap.get("attributes"));
            String eventType = stringValue(eventMap.get("type"));
            long timestamp = parseLong(stringValue(eventMap.get("timestamp")), System.currentTimeMillis());
            String eventPageUrl = firstNonBlank(
                    attributes.get("pageUrl"),
                    attributes.get("currentUrl"),
                    attributes.get("url"),
                    currentPageUrl
            );
            String eventPageId = firstNonBlank(attributes.get("pageId"), pageIdFromUrl(eventPageUrl), currentPageId);
            eventBuffer.add(new BiDiRuntimeEvent(eventType, eventPageId, eventPageUrl, attributes, timestamp));
        }
    }

    public void stop(WebDriver driver) {
        if (!started || !isAvailable()) {
            return;
        }
        drain(driver);
        addEvent("bidi.session.stop", Map.of(
                "url", currentPageUrl,
                "pageUrl", currentPageUrl,
                "trigger", "discovery-session"
        ));
        started = false;
    }

    private void installInstrumentation(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript(INSTALL_SCRIPT);
        } catch (RuntimeException exception) {
            addEvent("log.entryAdded", Map.of(
                    "level", "WARN",
                    "message", "Failed to install browser-side BiDi instrumentation: " + exception.getMessage(),
                    "pageUrl", currentPageUrl
            ));
        }
    }

    private void addEvent(String eventType, Map<String, String> attributes) {
        if (!isAvailable()) {
            return;
        }
        String pageUrl = firstNonBlank(attributes.get("pageUrl"), attributes.get("url"), currentPageUrl);
        String pageId = firstNonBlank(attributes.get("pageId"), pageIdFromUrl(pageUrl), currentPageId);
        eventBuffer.add(new BiDiRuntimeEvent(eventType, pageId, pageUrl, attributes, System.currentTimeMillis()));
    }

    private Map<String, String> toAttributes(Object rawAttributes) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (rawAttributes instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = stringValue(entry.getKey());
                if (!key.isBlank()) {
                    attributes.put(key, stringValue(entry.getValue()));
                }
            }
        }
        return attributes;
    }

    private long parseLong(String value, long fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String pageIdFromUrl(String url) {
        try {
            URI uri = URI.create(clean(url));
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return "home-page";
            }
            String normalized = path.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
            return normalized.isBlank() ? "page" : normalized.toLowerCase(Locale.ROOT);
        } catch (Exception exception) {
            return "";
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final String INSTALL_SCRIPT = """
            (function() {
              if (window.__agentLabBiDiInstalled) {
                if (window.__agentLabBiDiPush) {
                  window.__agentLabBiDiPush('browsingContext.lifecycle', {
                    name: 'already-installed',
                    pageUrl: String(window.location.href),
                    currentUrl: String(window.location.href),
                    readyState: String(document.readyState || '')
                  });
                }
                return;
              }
              window.__agentLabBiDiInstalled = true;
              window.__agentLabBiDiEvents = [];
              window.__agentLabBiDiPush = function(type, attributes) {
                try {
                  attributes = attributes || {};
                  attributes.pageUrl = attributes.pageUrl || String(window.location.href);
                  attributes.currentUrl = String(window.location.href);
                  window.__agentLabBiDiEvents.push({
                    type: String(type || ''),
                    timestamp: Date.now(),
                    attributes: attributes
                  });
                } catch (ignored) {}
              };
              window.__agentLabBiDiDrain = function() {
                var events = window.__agentLabBiDiEvents || [];
                window.__agentLabBiDiEvents = [];
                return events;
              };
              window.__agentLabBiDiPush('browsingContext.lifecycle', {
                name: 'installed',
                pageUrl: String(window.location.href),
                readyState: String(document.readyState || '')
              });
              var previousUrl = String(window.location.href);
              function pushNavigation(kind, fromUrl) {
                var toUrl = String(window.location.href);
                window.__agentLabBiDiPush('browsingContext.navigationStarted', {
                  fromUrl: String(fromUrl || previousUrl || ''),
                  url: toUrl,
                  trigger: kind,
                  sameDocument: 'true'
                });
                previousUrl = toUrl;
              }
              var originalPushState = history.pushState;
              history.pushState = function() {
                var fromUrl = String(window.location.href);
                var result = originalPushState.apply(this, arguments);
                pushNavigation('history.pushState', fromUrl);
                return result;
              };
              var originalReplaceState = history.replaceState;
              history.replaceState = function() {
                var fromUrl = String(window.location.href);
                var result = originalReplaceState.apply(this, arguments);
                pushNavigation('history.replaceState', fromUrl);
                return result;
              };
              window.addEventListener('popstate', function() { pushNavigation('history.popstate', previousUrl); });
              window.addEventListener('hashchange', function(event) {
                window.__agentLabBiDiPush('browsingContext.navigationStarted', {
                  fromUrl: String(event.oldURL || previousUrl || ''),
                  url: String(event.newURL || window.location.href),
                  trigger: 'hashchange',
                  sameDocument: 'true'
                });
                previousUrl = String(window.location.href);
              });
              ['log', 'info', 'warn', 'error'].forEach(function(level) {
                var original = console[level];
                if (typeof original !== 'function') {
                  return;
                }
                console[level] = function() {
                  try {
                    var message = Array.prototype.slice.call(arguments).map(function(arg) {
                      if (typeof arg === 'string') {
                        return arg;
                      }
                      try {
                        return JSON.stringify(arg);
                      } catch (ignored) {
                        return String(arg);
                      }
                    }).join(' ');
                    window.__agentLabBiDiPush('log.entryAdded', {
                      level: level.toUpperCase(),
                      message: message
                    });
                  } catch (ignored) {}
                  return original.apply(console, arguments);
                };
              });
              if (window.fetch) {
                var originalFetch = window.fetch;
                window.fetch = function(input, init) {
                  var url = typeof input === 'string' ? input : String((input && input.url) || '');
                  var method = String((init && init.method) || (input && input.method) || 'GET').toUpperCase();
                  window.__agentLabBiDiPush('network.requestWillBeSent', {
                    method: method,
                    url: url,
                    resourceType: 'fetch'
                  });
                  return originalFetch.apply(this, arguments).then(function(response) {
                    window.__agentLabBiDiPush('network.responseCompleted', {
                      method: method,
                      url: String(response.url || url),
                      status: String(response.status || 0),
                      resourceType: 'fetch'
                    });
                    return response;
                  }).catch(function(error) {
                    window.__agentLabBiDiPush('network.responseCompleted', {
                      method: method,
                      url: url,
                      status: '0',
                      resourceType: 'fetch',
                      error: String(error && error.message || error || '')
                    });
                    throw error;
                  });
                };
              }
              if (window.XMLHttpRequest) {
                var originalOpen = XMLHttpRequest.prototype.open;
                var originalSend = XMLHttpRequest.prototype.send;
                XMLHttpRequest.prototype.open = function(method, url) {
                  this.__agentLabMethod = String(method || 'GET').toUpperCase();
                  this.__agentLabUrl = String(url || '');
                  return originalOpen.apply(this, arguments);
                };
                XMLHttpRequest.prototype.send = function() {
                  var xhr = this;
                  var method = xhr.__agentLabMethod || 'GET';
                  var url = xhr.__agentLabUrl || '';
                  window.__agentLabBiDiPush('network.requestWillBeSent', {
                    method: method,
                    url: url,
                    resourceType: 'xhr'
                  });
                  xhr.addEventListener('loadend', function() {
                    window.__agentLabBiDiPush('network.responseCompleted', {
                      method: method,
                      url: url,
                      status: String(xhr.status || 0),
                      resourceType: 'xhr'
                    });
                  });
                  return originalSend.apply(this, arguments);
                };
              }
              var mutationCount = 0;
              var mutationTimer = null;
              if (window.MutationObserver && document.documentElement) {
                var observer = new MutationObserver(function(mutations) {
                  mutationCount += mutations.length;
                  if (mutationTimer) {
                    clearTimeout(mutationTimer);
                  }
                  mutationTimer = setTimeout(function() {
                    window.__agentLabBiDiPush('dom.mutation', {
                      mutationType: 'childListOrAttributes',
                      target: 'document',
                      count: String(mutationCount),
                      pageUrl: String(window.location.href)
                    });
                    mutationCount = 0;
                  }, 250);
                });
                observer.observe(document.documentElement, {
                  childList: true,
                  subtree: true,
                  attributes: true
                });
              }
              window.addEventListener('load', function() {
                window.__agentLabBiDiPush('browsingContext.lifecycle', {
                  name: 'load',
                  pageUrl: String(window.location.href),
                  readyState: String(document.readyState || '')
                });
              });
            })();
            """;
}
