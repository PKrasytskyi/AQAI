package ua.demo.agentlab.validation.smoke;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

/** Invokes only the public API of a compiled generated Page Object. */
public final class GeneratedPomRuntimeInvoker {

    public void open(Object page) {
        invoke(required(page, method -> method.getParameterCount() == 0
                && method.getReturnType() == void.class
                && method.getName().startsWith("open"), "open page method"), page);
    }

    public void authenticate(Object page, String username, String password) {
        Optional<Method> aggregate = methods(page).filter(method -> method.getParameterCount() == 2
                        && parametersAreStrings(method)
                        && normalized(method.getName()).equals("login"))
                .findFirst();
        if (aggregate.isPresent()) {
            invoke(aggregate.get(), page, username, password);
            return;
        }
        invoke(requiredStringAction(page, "username"), page, username);
        invoke(requiredStringAction(page, "password"), page, password);
        invoke(required(page, method -> method.getParameterCount() == 0
                        && method.getReturnType() == void.class
                        && !normalized(method.getName()).startsWith("open")
                        && containsAny(normalized(method.getName()), "clicklogin", "submitlogin", "submitauthentication"),
                "authentication submit method"), page);
    }

    public boolean hasOpenUserMenu(Object page) {
        return action(page, "openusermenu").isPresent();
    }

    public void openUserMenu(Object page) {
        invoke(action(page, "openusermenu")
                .orElseThrow(() -> missing(page, "openUserMenu action")), page);
    }

    public boolean logoutVisible(Object page) {
        Method assertion = required(page, method -> method.getParameterCount() == 0
                        && method.getReturnType() == boolean.class
                        && normalized(method.getName()).contains("logout")
                        && containsAny(normalized(method.getName()), "visible", "displayed", "present"),
                "logout visibility assertion");
        return (Boolean) invoke(assertion, page);
    }

    public void logout(Object page) {
        invoke(action(page, "logout").orElseThrow(() -> missing(page, "logout action")), page);
    }

    public boolean routeMatches(Object page, String route) {
        String routeToken = normalized(route);
        Method method = methods(page)
                .filter(candidate -> candidate.getParameterCount() == 0
                        && candidate.getReturnType() == boolean.class
                        && normalized(candidate.getName()).startsWith("urlcontains"))
                .max(Comparator.comparingInt(candidate -> routeScore(candidate, routeToken)))
                .orElseThrow(() -> missing(page, "URL route assertion"));
        return (Boolean) invoke(method, page);
    }

    private Method requiredStringAction(Object page, String token) {
        return required(page, method -> method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == String.class
                        && normalized(method.getName()).contains(token),
                token + " input action");
    }

    private Optional<Method> action(Object page, String normalizedName) {
        return methods(page).filter(method -> method.getParameterCount() == 0
                        && method.getReturnType() == void.class
                        && normalized(method.getName()).equals(normalizedName))
                .findFirst();
    }

    private Method required(Object page, java.util.function.Predicate<Method> predicate, String description) {
        return methods(page).filter(predicate).findFirst()
                .orElseThrow(() -> missing(page, description));
    }

    private java.util.stream.Stream<Method> methods(Object page) {
        if (page == null) {
            throw new IllegalArgumentException("generated POM instance cannot be null");
        }
        return Arrays.stream(page.getClass().getMethods())
                .filter(method -> method.getDeclaringClass() == page.getClass());
    }

    private boolean parametersAreStrings(Method method) {
        return Arrays.stream(method.getParameterTypes()).allMatch(type -> type == String.class);
    }

    private int routeScore(Method method, String routeToken) {
        String name = normalized(method.getName());
        return !routeToken.isBlank() && name.contains(routeToken) ? routeToken.length() : 0;
    }

    private Object invoke(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access generated POM method " + method.getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            String message = cause.getMessage() == null || cause.getMessage().isBlank()
                    ? cause.getClass().getSimpleName()
                    : cause.getMessage();
            throw new IllegalStateException("Generated POM method " + method.getName() + " failed: " + message, cause);
        }
    }

    private IllegalStateException missing(Object page, String description) {
        String type = page == null ? "<null>" : page.getClass().getName();
        return new IllegalStateException("Compiled generated POM " + type + " is missing " + description);
    }

    private boolean containsAny(String value, String... fragments) {
        return Arrays.stream(fragments).anyMatch(value::contains);
    }

    private String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
