package ua.demo.agentlab.ui.discovery.policy;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.RuntimeProperties;

/** Applies runtime discovery mode without embedding product routes or page names. */
public final class DiscoveryCrawlPolicyResolver {
    private final RuntimeProperties properties;

    public DiscoveryCrawlPolicyResolver() {
        this(new RuntimeProperties());
    }

    DiscoveryCrawlPolicyResolver(RuntimeProperties properties) {
        this.properties = properties == null ? new RuntimeProperties() : properties;
    }

    public DiscoveryCrawlPolicy resolve(ProjectProfile profile) {
        DiscoveryCrawlPolicy base = DiscoveryCrawlPolicy.defaultPolicy(profile);
        String mode = properties.readValue("spa.discovery.mode", "targeted").trim().toLowerCase();
        if (!"targeted".equals(mode)) {
            return base;
        }
        int explicitRoutes = Math.max(1, base.startRoutes().size());
        return new DiscoveryCrawlPolicy(
                base.startRoutes(),
                1,
                explicitRoutes + 4,
                base.sameDomainOnly(),
                false,
                false,
                base.allowAuthentication(),
                base.allowFormSubmit(),
                base.stopOnLogout(),
                base.blockedActionKeywords(),
                base.blockedRouteKeywords()
        );
    }
}
