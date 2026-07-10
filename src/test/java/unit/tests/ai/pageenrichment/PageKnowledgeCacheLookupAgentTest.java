package unit.tests.ai.pageenrichment;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.pageenrichment.agent.PageKnowledgeCacheLookupAgent;
import ua.demo.agentlab.ai.pageenrichment.agent.PageKnowledgeCacheLookupInput;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheQueryService;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeFingerprintCalculator;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import java.util.List;
import java.util.Optional;

public class PageKnowledgeCacheLookupAgentTest {

    @Test
    public void acceptsStableCacheRecordAtConfirmedLocatorConfidenceThreshold() {
        MappedPage page = loginPage();
        String fingerprint = new PageKnowledgeFingerprintCalculator().fingerprint(page);
        PageModelEnrichmentRecord cachedRecord = new PageModelEnrichmentRecord(
                page.pageId(),
                page.pageName(),
                page.urlPattern(),
                "authentication",
                "Cached login page enrichment",
                List.of("login"),
                List.of("name=username (stability=0.88, sameOrigin=true, element=INPUT)"),
                List.of("Application is available"),
                List.of("Login page route contains the project login route."),
                List.of(),
                List.of(),
                List.of("REQ-001"),
                java.util.Map.of(),
                java.util.Map.of(),
                0.78d,
                "db-cache"
        );
        PageKnowledgeCacheLookupAgent agent = new PageKnowledgeCacheLookupAgent(
                new FakePageKnowledgeCacheQueryService(page.pageId(), fingerprint, cachedRecord)
        );

        var output = agent.execute(new PageKnowledgeCacheLookupInput(
                null,
                new MappedUiKnowledge(List.of(page), List.of(), List.of(), List.of(), List.of()),
                null,
                runMetadata()
        ), WorkflowRunEnvelope.create("test", null));

        Assert.assertEquals(output.result().hitCount(), 1);
        Assert.assertEquals(output.result().cachedRecords().get(0).pageName(), "LoginPage");
    }

    private MappedPage loginPage() {
        return new MappedPage(
                "web-index-php-auth-login",
                "LoginPage",
                "AUTHENTICATION",
                "https://example.test/web/index.php/auth/login",
                "/auth/login",
                "OrangeHRM",
                List.of(),
                List.of(new MappedElement(
                        "username",
                        "username",
                        "INPUT",
                        "input",
                        "Username",
                        true,
                        true,
                        List.of(new LocatorCandidate(
                                LocatorStrategy.NAME,
                                "username",
                                0.88d,
                                "test",
                                "input",
                                "",
                                "",
                                "",
                                "example.test",
                                true,
                                true,
                                true,
                                List.of()
                        )),
                        List.of("TYPE"),
                        0.90d
                )),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
    }

    private KnowledgeRunMetadata runMetadata() {
        return new KnowledgeRunMetadata(
                "run",
                "orangeHRM",
                "base",
                "requirements",
                "discovery",
                KnowledgeRunMetadata.CURRENT_SCHEMA_VERSION,
                "2026-07-10T00:00:00Z",
                "test",
                1.0d
        );
    }

    private static final class FakePageKnowledgeCacheQueryService extends PageKnowledgeCacheQueryService {
        private final String expectedPageId;
        private final String expectedFingerprint;
        private final PageModelEnrichmentRecord record;

        private FakePageKnowledgeCacheQueryService(
                String expectedPageId,
                String expectedFingerprint,
                PageModelEnrichmentRecord record
        ) {
            super(new DisabledNeo4jRuntimeConfig());
            this.expectedPageId = expectedPageId;
            this.expectedFingerprint = expectedFingerprint;
            this.record = record;
        }

        @Override
        public Optional<PageModelEnrichmentRecord> findCachedEnrichment(
                KnowledgeRunMetadata runMetadata,
                String pageId,
                String pageFingerprintHash
        ) {
            if (expectedPageId.equals(pageId) && expectedFingerprint.equals(pageFingerprintHash)) {
                return Optional.of(record);
            }
            return Optional.empty();
        }
    }

    private static final class DisabledNeo4jRuntimeConfig implements Neo4jRuntimeConfig {
        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public String httpUrl() {
            return "http://localhost:7474";
        }

        @Override
        public String database() {
            return "neo4j";
        }

        @Override
        public String username() {
            return "neo4j";
        }

        @Override
        public String password() {
            return "password";
        }
    }
}
