package ua.demo.agentlab.validation.smoke;

import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.core.config.UiRuntimeConfig;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/** Loads the Page Object class emitted and compiled by the current workflow run. */
public final class GeneratedPomRuntimeLoader {

    public Object load(GeneratedSourceFile source, WebDriver driver, UiRuntimeConfig runtimeConfig) {
        if (source == null) {
            throw new IllegalArgumentException("generated source cannot be null");
        }
        String className = qualifiedName(source);
        try {
            URL[] runtimeRoots = {
                    Path.of("target", "test-classes").toAbsolutePath().normalize().toUri().toURL(),
                    Path.of("target", "classes").toAbsolutePath().normalize().toUri().toURL()
            };
            ClassLoader parent = Thread.currentThread().getContextClassLoader();
            try (URLClassLoader loader = new URLClassLoader(runtimeRoots, parent)) {
                Class<?> pageClass = Class.forName(className, true, loader);
                return pageClass.getConstructor(WebDriver.class, UiRuntimeConfig.class)
                        .newInstance(driver, runtimeConfig);
            }
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new IllegalStateException("Cannot load compiled generated POM " + className + ": "
                    + rootMessage(exception), exception);
        }
    }

    private String qualifiedName(GeneratedSourceFile source) {
        String packageName = source.packageName() == null ? "" : source.packageName().trim();
        String className = source.className() == null ? "" : source.className().trim();
        if (packageName.isBlank() || className.isBlank()) {
            throw new IllegalArgumentException("generated source package and class name are required");
        }
        return packageName + "." + className;
    }

    private String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
