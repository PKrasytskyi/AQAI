package ua.demo.agentlab.ui.discovery.identity;

import java.util.Locale;

public enum CanonicalPageType {
    LANDING("generic", "HomePage", "home"),
    LISTING("list", "ListPage", "list"),
    DETAILS("detail", "DetailPage", "detail"),
    CART("container", "ContainerPage", "container"),
    AUTHENTICATION("authentication", "LoginPage", "login"),
    REGISTRATION("registration", "RegistrationPage", "registration"),
    RECOVERY("recovery", "RecoveryPage", "recovery"),
    AUTHENTICATED_AREA("authenticated-area", "SecureAreaPage", "authenticated"),
    SECURITY("security", "SecurityPage", "security"),
    DASHBOARD("overview", "DashboardPage", "dashboard"),
    FORM("form", "FormPage", "form"),
    SEARCH("search", "SearchPage", "search"),
    GENERIC("generic", "GenericPage", "page");

    private final String mappedType;
    private final String defaultClassName;
    private final String defaultAlias;

    CanonicalPageType(String mappedType, String defaultClassName, String defaultAlias) {
        this.mappedType = mappedType;
        this.defaultClassName = defaultClassName;
        this.defaultAlias = defaultAlias;
    }

    public String mappedType() {
        return mappedType;
    }

    public String defaultClassName() {
        return defaultClassName;
    }

    public String defaultAlias() {
        return defaultAlias;
    }

    public static CanonicalPageType fromLegacyPageName(String pageName) {
        String normalized = normalize(pageName);
        return switch (normalized) {
            case "homepage", "landingpage" -> LANDING;
            case "listingpage", "collectionspage", "catalogpage" -> LISTING;
            case "detailspage", "productdetailspage", "itemdetailspage" -> DETAILS;
            case "containerpage", "cartpage", "basketpage" -> CART;
            case "loginpage", "signinpage", "authenticationpage" -> AUTHENTICATION;
            case "registrationpage", "signuppage", "registerpage" -> REGISTRATION;
            case "recoverypage", "resetpage", "forgotpasswordpage" -> RECOVERY;
            case "secureareapage", "authenticatedpage" -> AUTHENTICATED_AREA;
            case "securitypage", "challengepage" -> SECURITY;
            case "dashboardpage", "overviewpage", "accountpage" -> DASHBOARD;
            case "formpage" -> FORM;
            case "searchpage", "searchresultspage" -> SEARCH;
            default -> GENERIC;
        };
    }

    public static CanonicalPageType fromMappedType(String pageType) {
        String normalized = normalize(pageType);
        return switch (normalized) {
            case "landing" -> LANDING;
            case "listing", "publiccatalog", "catalog" -> LISTING;
            case "details", "detailview" -> DETAILS;
            case "container", "cart", "basket" -> CART;
            case "authentication", "authpage", "login" -> AUTHENTICATION;
            case "registration", "entitycreate" -> REGISTRATION;
            case "recovery" -> RECOVERY;
            case "authenticatedarea", "securearea", "secure", "authenticated" -> AUTHENTICATED_AREA;
            case "security" -> SECURITY;
            case "overview", "dashboard" -> DASHBOARD;
            case "form", "transactionalform" -> FORM;
            case "search" -> SEARCH;
            default -> GENERIC;
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }
}
