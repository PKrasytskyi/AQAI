package ua.demo.agentlab.ui.capability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Product-neutral capability vocabulary shared by multi-page and SPA evidence flows. */
public final class UiCapabilityRegistry {

    private final Map<String, UiCapabilityContract> capabilities;

    public UiCapabilityRegistry() {
        this(defaultContracts());
    }

    public UiCapabilityRegistry(List<UiCapabilityContract> contracts) {
        Map<String, UiCapabilityContract> indexed = new LinkedHashMap<>();
        for (UiCapabilityContract contract : contracts == null ? List.<UiCapabilityContract>of() : contracts) {
            indexed.put(normalize(contract.id()), contract);
            contract.aliases().forEach(alias -> indexed.put(normalize(alias), contract));
        }
        capabilities = Map.copyOf(indexed);
    }

    public Optional<UiCapabilityContract> resolve(String value) {
        return Optional.ofNullable(capabilities.get(normalize(value)));
    }

    public boolean supports(String value) {
        return resolve(value).isPresent();
    }

    private static List<UiCapabilityContract> defaultContracts() {
        return List.of(
                page("AUTHENTICATION", "LOGIN"),
                page("AUTHENTICATED_AREA", "PROTECTED_RESOURCE", "SECURE_AREA", "DASHBOARD"),
                page("NAVIGATION", "MODULE_NAVIGATION", "NAVIGATION_HUB"),
                page("RECORD_LIST", "RESULTS_COLLECTION"),
                page("PASSWORD_RECOVERY", "RECOVERY"),
                component("FORM", "DATA_ENTRY"),
                component("SELECTION", "SELECT_OPTION", "CHECKBOX"),
                component("FILTER", "SEARCH"),
                component("USER_MENU"),
                component("MODAL"),
                dynamic("DYNAMIC_ELEMENT_MANAGEMENT", "DYNAMIC_COMPONENT"),
                dynamic("DYNAMIC_CONTROL"),
                dynamic("DYNAMIC_LOADING", "ASYNC_CONTENT"),
                component("HOVER"),
                component("SLIDER_INTERACTION", "SLIDER"),
                browser("HTTP_AUTHENTICATION", "BASIC_AUTH"),
                browser("JAVASCRIPT_DIALOG", "ALERT", "CONFIRM", "PROMPT"),
                browser("FILE_UPLOAD", "UPLOAD"),
                browser("WINDOW_MANAGEMENT", "WINDOW", "TAB_MANAGEMENT")
        );
    }

    private static UiCapabilityContract page(String id, String... aliases) {
        return contract(id, UiCapabilityKind.PAGE, false, aliases);
    }

    private static UiCapabilityContract component(String id, String... aliases) {
        return contract(id, UiCapabilityKind.COMPONENT, false, aliases);
    }

    private static UiCapabilityContract dynamic(String id, String... aliases) {
        return contract(id, UiCapabilityKind.DYNAMIC_STATE, false, aliases);
    }

    private static UiCapabilityContract browser(String id, String... aliases) {
        return contract(id, UiCapabilityKind.BROWSER_NATIVE, true, aliases);
    }

    private static UiCapabilityContract contract(
            String id,
            UiCapabilityKind kind,
            boolean browserAdapter,
            String... aliases
    ) {
        return new UiCapabilityContract(id, kind, List.of(aliases), browserAdapter);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
