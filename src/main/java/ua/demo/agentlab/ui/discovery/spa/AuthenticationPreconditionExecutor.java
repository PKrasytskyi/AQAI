package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationService;
import ua.demo.agentlab.ui.discovery.selenium.auth.RouteProtectionResolver;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;

import java.util.List;
import java.util.Objects;

/** Satisfies only the authentication precondition for a target page. */
public final class AuthenticationPreconditionExecutor {

    private final DiscoveryAuthenticationService authentication;
    private final RouteProtectionResolver routeProtectionResolver;

    public AuthenticationPreconditionExecutor(
            DiscoveryAuthenticationService authentication,
            RouteProtectionResolver routeProtectionResolver
    ) {
        this.authentication = Objects.requireNonNull(authentication, "authentication");
        this.routeProtectionResolver = Objects.requireNonNull(routeProtectionResolver, "routeProtectionResolver");
    }

    public AuthenticationPreconditionResult satisfy(
            WebDriver driver,
            ProjectProfile profile,
            SpaPageInventory page,
            List<TargetedActionVerification> actions,
            LiveAuthenticationState state
    ) {
        boolean required = routeProtectionResolver.isDeclaredProtected(profile, page, actions);
        if (!required || state.authenticated()) {
            return AuthenticationPreconditionResult.satisfied(required, false);
        }
        String target = join(profile.baseUrl(), profile.authenticatedRoute());
        var result = authentication.authenticate(driver, profile, target);
        if (!result.success()) {
            return AuthenticationPreconditionResult.failed("live authentication failed: " + result.reason());
        }
        state.markAuthenticated();
        return AuthenticationPreconditionResult.satisfied(true, true);
    }

    public boolean redirectedToLogin(ProjectProfile profile, String requestedRoute, String currentUrl) {
        return routeProtectionResolver.redirectedToLogin(profile, requestedRoute, currentUrl);
    }

    public boolean authenticateAfterRedirect(WebDriver driver, ProjectProfile profile, LiveAuthenticationState state) {
        var result = authentication.authenticate(driver, profile, join(profile.baseUrl(), profile.authenticatedRoute()));
        if (result.success()) state.markAuthenticated();
        return result.success();
    }

    private String join(String baseUrl, String route) {
        return baseUrl.replaceAll("/+$", "") + "/" + route.replaceAll("^/+", "");
    }

    public record AuthenticationPreconditionResult(
            boolean required,
            boolean satisfied,
            boolean authenticatedNow,
            String reason
    ) {
        public static AuthenticationPreconditionResult satisfied(boolean required, boolean authenticatedNow) {
            return new AuthenticationPreconditionResult(required, true, authenticatedNow, "satisfied");
        }

        public static AuthenticationPreconditionResult failed(String reason) {
            return new AuthenticationPreconditionResult(true, false, false, reason);
        }
    }
}
