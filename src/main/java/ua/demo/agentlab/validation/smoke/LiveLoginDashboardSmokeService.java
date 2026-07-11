package ua.demo.agentlab.validation.smoke;

/**
 * Compatibility adapter kept for existing callers. Runtime live smoke is now
 * capability/profile driven by {@link LiveCapabilitySmokeService}.
 */
@Deprecated(forRemoval = false)
public class LiveLoginDashboardSmokeService extends LiveCapabilitySmokeService {
}
