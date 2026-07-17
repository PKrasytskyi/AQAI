package unit.tests.ai;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.pageenrichment.service.PageEnrichmentPersistencePolicy;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PageEnrichmentPersistencePolicyTest {

    private final PageEnrichmentPersistencePolicy policy = new PageEnrichmentPersistencePolicy();

    @Test
    public void rejectsFailedFallbackAndUnownedEnrichment() {
        PageModelEnrichmentRecord valid = record("openai", List.of("REQ-1"));

        Assert.assertTrue(policy.isEligible(valid, Set.of()));
        Assert.assertFalse(policy.isEligible(valid, Set.of("login")));
        Assert.assertFalse(policy.isEligible(record("openai-fallback", List.of("REQ-1")), Set.of()));
        Assert.assertFalse(policy.isEligible(record("openai", List.of()), Set.of()));
    }

    private PageModelEnrichmentRecord record(String source, List<String> requirements) {
        return new PageModelEnrichmentRecord(
                "login", "LoginPage", "/login", "authentication", "Login form",
                List.of("authenticate"), List.of(), List.of(), List.of("route is /login"),
                List.of(), List.of(), requirements, Map.of(), Map.of(), 0.85d, source
        );
    }
}
