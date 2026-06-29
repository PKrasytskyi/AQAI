package ua.demo.agentlab.ui.catalog;

import ua.demo.agentlab.ui.discovery.identity.CanonicalPageType;

public enum PageCapability {
    NAVIGATION("HomePage", CanonicalPageType.LANDING),
    AUTHENTICATION("LoginPage", CanonicalPageType.AUTHENTICATION),
    AUTHENTICATED_AREA("SecureAreaPage", CanonicalPageType.AUTHENTICATED_AREA),
    FORM("FormPage", CanonicalPageType.FORM),
    RECORD_LIST("ListPage", CanonicalPageType.LISTING),
    RECORD_DETAILS("DetailPage", CanonicalPageType.DETAILS),
    CONTAINER("ContainerPage", CanonicalPageType.CART),
    REGISTRATION("RegistrationPage", CanonicalPageType.REGISTRATION),
    RECOVERY("RecoveryPage", CanonicalPageType.RECOVERY),
    SECURITY("SecurityPage", CanonicalPageType.SECURITY),
    DASHBOARD("DashboardPage", CanonicalPageType.DASHBOARD),
    GENERIC("GenericPage", CanonicalPageType.GENERIC);

    private final String defaultPageName;
    private final CanonicalPageType canonicalPageType;

    PageCapability(String defaultPageName, CanonicalPageType canonicalPageType) {
        this.defaultPageName = defaultPageName;
        this.canonicalPageType = canonicalPageType;
    }

    public String defaultPageName() {
        return defaultPageName;
    }

    public CanonicalPageType canonicalPageType() {
        return canonicalPageType;
    }
}
