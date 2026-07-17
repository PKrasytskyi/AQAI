package ua.demo.agentlab.ui.discovery.classification;

import ua.demo.agentlab.ui.discovery.identity.AliasRegistry;
import ua.demo.agentlab.ui.discovery.identity.CanonicalPageType;
import ua.demo.agentlab.ui.discovery.identity.DefaultAliasRegistry;
import ua.demo.agentlab.ui.discovery.identity.DefaultPageNamingResolver;
import ua.demo.agentlab.ui.discovery.identity.PageIdentity;
import ua.demo.agentlab.ui.discovery.identity.PageNamingResolver;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredField;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredInteractiveElement;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredTransition;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RuleBasedPageClassificationService implements PageClassificationService {

    private final AliasRegistry aliasRegistry;
    private final PageNamingResolver pageNamingResolver;

    public RuleBasedPageClassificationService() {
        this(new DefaultAliasRegistry(), null);
    }

    public RuleBasedPageClassificationService(AliasRegistry aliasRegistry, PageNamingResolver pageNamingResolver) {
        if (aliasRegistry == null) {
            throw new IllegalArgumentException("aliasRegistry cannot be null");
        }
        this.aliasRegistry = aliasRegistry;
        this.pageNamingResolver = pageNamingResolver == null
                ? new DefaultPageNamingResolver(aliasRegistry)
                : pageNamingResolver;
    }

    @Override
    public PageClassificationResult classifyPage(DiscoveredPageSnapshot snapshot, List<DiscoveredUiPage> knownPages) {
        String route = toRelativeRoute(snapshot.url()).toLowerCase(Locale.ROOT);
        Set<String> capabilities = new LinkedHashSet<>(snapshot.capabilities());

        for (DiscoveredUiPage page : knownPages) {
            if (page.route().equalsIgnoreCase(route)) {
                capabilities.addAll(page.capabilities());
                PageIdentity identity = page.pageIdentity() == null
                        ? pageNamingResolver.resolve(
                                page.canonicalPageType(),
                                page.route(),
                                snapshot.title(),
                                snapshot.headings(),
                                new ArrayList<>(capabilities),
                                knownPages
                        )
                        : page.pageIdentity();
                return buildResult(
                        identity.canonicalPageType(),
                        snapshot,
                        new ArrayList<>(capabilities),
                        knownPages,
                        "Matched known route from discovery skeleton",
                        identity
                );
            }
        }

        if (hasListingSignals(snapshot, route)) {
            capabilities.add("listing");
            capabilities.add("public-catalog");
            capabilities.add("details-navigation");
            return buildResult(CanonicalPageType.LISTING, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Listing signals detected from route, headings, and repeated item links");
        }

        if (hasCartSignals(snapshot, route)) {
            capabilities.add("cart");
            return buildResult(CanonicalPageType.CART, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Cart signals detected from route, headings, or cart-specific actions");
        }

        if (hasRecoverySignals(snapshot, route)) {
            capabilities.add("recovery");
            return buildResult(CanonicalPageType.RECOVERY, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Recovery signals detected from route or recovery-specific form content");
        }

        if (hasRegistrationSignals(snapshot, route)) {
            capabilities.add("entity-create");
            return buildResult(CanonicalPageType.REGISTRATION, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Registration or sign-up signals detected");
        }

        if (hasAuthenticationSignals(snapshot, route)) {
            capabilities.add("auth-page");
            return buildResult(CanonicalPageType.AUTHENTICATION, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Authentication page detected from login route, headings, and focused sign-in form");
        }

        if (hasSecuritySignals(snapshot, route)) {
            capabilities.add("security");
            return buildResult(CanonicalPageType.SECURITY, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Security checkpoint signals detected from headings, route, or capabilities");
        }

        if (hasOverviewSignals(snapshot, route)) {
            capabilities.add("list-page");
            return buildResult(CanonicalPageType.DASHBOARD, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Overview or authenticated area signals detected");
        }

        // A dashboard commonly contains generic "view" links. Route/authenticated evidence is more specific
        // than those incidental details signals, so keep the page capability stable for later ownership slicing.
        if (hasDetailsSignals(snapshot, route)) {
            capabilities.add("detail-view");
            return buildResult(CanonicalPageType.DETAILS, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Details signals detected from route, headings, or page capabilities");
        }

        if (hasGenericFormSignals(snapshot, route)) {
            capabilities.add("transactional-form");
            return buildResult(CanonicalPageType.FORM, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Non-auth multi-field form detected");
        }

        if ("/".equals(route) || route.isBlank()) {
            capabilities.add("landing");
            return buildResult(CanonicalPageType.LANDING, snapshot, new ArrayList<>(capabilities), knownPages,
                    "Default landing route detected");
        }

        capabilities.add("generic");
        return buildResult(CanonicalPageType.GENERIC, snapshot, new ArrayList<>(capabilities), knownPages,
                "No strong signals detected, defaulted to generic page");
    }

    @Override
    public String inferFlowType(
            DiscoveredTransition transition,
            DiscoveredPageSnapshot sourcePage,
            DiscoveredPageSnapshot targetPage,
            String targetPageName
    ) {
        String actionLabel = transition.actionLabel() == null ? "" : transition.actionLabel().toLowerCase(Locale.ROOT);
        String toUrl = transition.toUrl() == null ? "" : transition.toUrl().toLowerCase(Locale.ROOT);
        String targetText = summarize(targetPage);
        String sourceText = summarize(sourcePage);

        if ("RecoveryPage".equals(targetPageName)
                || containsAny(actionLabel + " " + toUrl + " " + targetText, "forgot", "reset", "recover", "lookup")) {
            return "RECOVERY";
        }
        if (containsAny(actionLabel, "logout", "log out", "sign out")
                || ("LoginPage".equals(targetPageName) && sourcePage != null && sourcePage.authenticatedArea())) {
            return "LOGOUT";
        }
        if ("ListPage".equals(targetPageName)) {
            return "NAVIGATE";
        }
        if ("DetailPage".equals(targetPageName) || containsAny(actionLabel + " " + targetText, "details", "view", "record")) {
            return "OPEN_DETAILS";
        }
        if ("RegistrationPage".equals(targetPageName)) {
            return "CREATE_ENTITY";
        }
        if ("SecurityPage".equals(targetPageName) || containsAny(targetText, "security", "verification", "challenge")) {
            return "SECURITY_CHALLENGE";
        }
        if ("FormPage".equals(targetPageName)) {
            return "SUBMIT_FORM";
        }
        if ("LoginPage".equals(targetPageName) && containsAny(actionLabel + " " + targetText + " " + sourceText, "login", "sign in", "authenticate")) {
            return "AUTHENTICATE";
        }
        if ("DashboardPage".equals(targetPageName) || "HomePage".equals(targetPageName)) {
            return "NAVIGATE";
        }
        return null;
    }

    private boolean hasListingSignals(DiscoveredPageSnapshot snapshot, String route) {
        return containsAny(route, "/collections", "/catalog", "/category", "/shop", "/recruitment", "/vacanc")
                || snapshot.capabilities().contains("listing")
                || snapshot.headings().stream().anyMatch(this::containsListingKeyword)
                || containsAny(summarize(snapshot), "recruitment", "vacancy", "vacancies", "search results")
                || hasMultipleRecordLinks(snapshot);
    }

    private boolean hasAuthenticationSignals(DiscoveredPageSnapshot snapshot, String route) {
        return containsAny(route, "login", "signin", "sign-in", "/account/login")
                || snapshot.capabilities().contains("authentication")
                || snapshot.forms().stream().anyMatch(this::isLoginForm)
                || containsAny(summarize(snapshot), "sign in", "login");
    }

    private boolean hasRecoverySignals(DiscoveredPageSnapshot snapshot, String route) {
        return containsAny(route, "recover", "forgot", "reset")
                || snapshot.capabilities().contains("recovery-entry")
                || (snapshot.modalVisible()
                    && hasEmailField(snapshot)
                    && !snapshot.forms().stream().anyMatch(this::hasPasswordField)
                    && containsAny(summarize(snapshot), "recover", "forgot", "reset", "lookup"));
    }

    private boolean hasCartSignals(DiscoveredPageSnapshot snapshot, String route) {
        return containsAny(route + " " + summarize(snapshot), "cart", "basket", "bag", "checkout")
                || snapshot.capabilities().contains("cart");
    }

    private boolean hasSecuritySignals(DiscoveredPageSnapshot snapshot, String route) {
        return containsAny(route + " " + summarize(snapshot), "security", "challenge", "verification", "otp", "mfa")
                || snapshot.capabilities().contains("security");
    }

    private boolean hasRegistrationSignals(DiscoveredPageSnapshot snapshot, String route) {
        return containsAny(route, "register", "signup", "sign-up", "/account/register")
                || snapshot.capabilities().contains("create")
                || containsAny(summarize(snapshot), "register", "registration", "sign up", "create account")
                || snapshot.forms().stream().anyMatch(this::isRegistrationForm);
    }

    private boolean hasDetailsSignals(DiscoveredPageSnapshot snapshot, String route) {
        return containsAny(route + " " + summarize(snapshot), "detail", "profile", "transaction", "record", "view")
                || snapshot.capabilities().contains("details")
                || snapshot.capabilities().contains("details-navigation");
    }

    private boolean hasOverviewSignals(DiscoveredPageSnapshot snapshot, String route) {
        return snapshot.authenticatedArea()
                || containsAny(route + " " + summarize(snapshot), "dashboard", "overview", "account")
                || snapshot.capabilities().contains("overview")
                || snapshot.capabilities().contains("authenticated-area");
    }

    private boolean hasGenericFormSignals(DiscoveredPageSnapshot snapshot, String route) {
        return !hasAuthenticationSignals(snapshot, route)
                && !hasListingSignals(snapshot, route)
                && snapshot.forms().stream().anyMatch(form -> form.fields().size() >= 2 && !form.submitActions().isEmpty());
    }

    private boolean hasPasswordField(ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredForm form) {
        return form.fields().stream().anyMatch(field -> "password".equalsIgnoreCase(field.fieldType()));
    }

    private boolean isLoginForm(ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredForm form) {
        boolean hasPassword = hasPasswordField(form);
        boolean hasIdentity = form.fields().stream().anyMatch(field ->
                "email".equalsIgnoreCase(field.fieldType())
                        || containsAny(lower(field.name()), "email", "user", "login")
                        || containsAny(lower(field.label()), "email", "user", "login")
        );

        return hasPassword && hasIdentity;
    }

    private boolean isRegistrationForm(ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredForm form) {
        Set<String> fieldSignals = new LinkedHashSet<>();
        form.fields().forEach(field -> {
            fieldSignals.add(lower(field.name()));
            fieldSignals.add(lower(field.label()));
            fieldSignals.add(lower(field.placeholder()));
        });

        boolean hasPassword = hasPasswordField(form);
        boolean hasName = fieldSignals.stream().anyMatch(value -> containsAny(value, "first", "last", "name"));
        boolean hasEmail = fieldSignals.stream().anyMatch(value -> value.contains("email"));

        return hasPassword && hasName && hasEmail;
    }

    private boolean hasEmailField(DiscoveredPageSnapshot snapshot) {
        return snapshot.forms().stream()
                .flatMap(form -> form.fields().stream())
                .anyMatch(this::isEmailField);
    }

    private boolean hasMultipleRecordLinks(DiscoveredPageSnapshot snapshot) {
        long recordLikeLinks = snapshot.links().stream()
                .filter(link -> {
                    String text = lower(link.visibleText());
                    String href = lower(link.href());
                    return containsAny(href, "/collections/", "/catalog/", "/category/", "/records/", "/items/")
                            || containsAny(text, "item", "record", "view", "details");
                })
                .count();
        return recordLikeLinks >= 2;
    }

    private boolean containsListingKeyword(String text) {
        return containsAny(lower(text), "collection", "collections", "catalog", "records", "items", "list",
                "recruitment", "vacancy", "vacancies", "search results");
    }

    private boolean isEmailField(DiscoveredField field) {
        String key = ((field.name() == null ? "" : field.name()) + " "
                + (field.label() == null ? "" : field.label()) + " "
                + (field.placeholder() == null ? "" : field.placeholder())).toLowerCase(Locale.ROOT);
        return "email".equalsIgnoreCase(field.fieldType()) || key.contains("email");
    }

    private String summarize(DiscoveredPageSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        parts.add(snapshot.title() == null ? "" : snapshot.title());
        parts.addAll(snapshot.headings());
        parts.addAll(snapshot.capabilities());
        parts.addAll(extractTexts(snapshot.links()));
        parts.addAll(extractTexts(snapshot.buttons()));
        return String.join(" ", parts).toLowerCase(Locale.ROOT);
    }

    private List<String> extractTexts(List<DiscoveredInteractiveElement> elements) {
        List<String> values = new ArrayList<>();
        for (DiscoveredInteractiveElement element : elements) {
            if (element.visibleText() != null && !element.visibleText().isBlank()) {
                values.add(element.visibleText());
            }
        }
        return values;
    }

    private String toRelativeRoute(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            return (path == null || path.isBlank()) ? "/" : path;
        } catch (Exception exception) {
            return url == null ? "/" : url;
        }
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private PageClassificationResult buildResult(
            CanonicalPageType canonicalPageType,
            DiscoveredPageSnapshot snapshot,
            List<String> capabilities,
            List<DiscoveredUiPage> knownPages,
            String reason
    ) {
        PageIdentity identity = pageNamingResolver.resolve(
                canonicalPageType,
                toRelativeRoute(snapshot.url()),
                snapshot.title(),
                snapshot.headings(),
                mergeWithAliases(capabilities, canonicalPageType),
                knownPages
        );
        return buildResult(canonicalPageType, snapshot, capabilities, knownPages, reason, identity);
    }

    private PageClassificationResult buildResult(
            CanonicalPageType canonicalPageType,
            DiscoveredPageSnapshot snapshot,
            List<String> capabilities,
            List<DiscoveredUiPage> knownPages,
            String reason,
            PageIdentity identity
    ) {
        List<String> mergedCapabilities = new ArrayList<>(capabilities);
        for (String alias : aliasRegistry.aliasesFor(
                canonicalPageType,
                toRelativeRoute(snapshot.url()),
                snapshot.title(),
                snapshot.headings(),
                capabilities
        )) {
            if (!mergedCapabilities.contains(alias)) {
                mergedCapabilities.add(alias);
            }
        }
        return new PageClassificationResult(
                identity.className(),
                mergedCapabilities,
                reason,
                canonicalPageType,
                identity
        );
    }

    private List<String> mergeWithAliases(List<String> capabilities, CanonicalPageType canonicalPageType) {
        List<String> merged = new ArrayList<>(capabilities);
        List<String> aliases = aliasRegistry.aliasesFor(canonicalPageType, "", "", List.of(), capabilities);
        for (String alias : aliases) {
            if (!merged.contains(alias)) {
                merged.add(alias);
            }
        }
        return merged;
    }
}
