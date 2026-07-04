package unit.tests.ui.discovery.runtime;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageEvidenceModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageStateHints;
import ua.demo.agentlab.ui.discovery.mapping.PageModelMappedPageMapper;
import ua.demo.agentlab.ui.discovery.runtime.NetworkSemanticEnricher;
import ua.demo.agentlab.ui.discovery.runtime.RuntimeEvidencePageModelMerger;
import ua.demo.agentlab.ui.discovery.runtime.SeleniumLogRuntimeEvidenceCollector;
import ua.demo.agentlab.ui.discovery.runtime.SpaStateTransitionDetector;
import ua.demo.agentlab.ui.discovery.runtime.feedback.RuntimeFeedbackAnalyzer;
import ua.demo.agentlab.ui.discovery.runtime.model.NavigationEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.NetworkResponseEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.runtime.model.SemanticNetworkEvidence;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelBuilder;
import ua.demo.agentlab.ui.discovery.semanticgraph.SemanticGraphBuilder;
import ua.demo.agentlab.ui.discovery.semanticgraph.SemanticGraphMappedKnowledgeEnricher;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserNetworkCall;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredTransition;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.List;
import java.util.Map;

public class RuntimeEvidencePipelineTest {

    @Test
    public void networkEnricherClassifiesAuthenticationCalls() {
        NetworkResponseEvent response = response("login", "POST", "https://app.test/api/login", 200, "XHR");

        List<SemanticNetworkEvidence> evidence = new NetworkSemanticEnricher().enrich(List.of(response));

        Assert.assertEquals(evidence.size(), 1);
        Assert.assertEquals(evidence.get(0).operation(), "AUTHENTICATE");
        Assert.assertEquals(evidence.get(0).businessIntent(), "authentication");
        Assert.assertTrue(evidence.get(0).confidence() >= 0.90d);
    }

    @Test
    public void networkEnricherFiltersStaticAssets() {
        NetworkResponseEvent image = response("dashboard", "GET", "https://app.test/assets/logo.png", 200, "Image");

        List<SemanticNetworkEvidence> evidence = new NetworkSemanticEnricher().enrich(List.of(image));

        Assert.assertTrue(evidence.isEmpty());
    }

    @Test
    public void spaDetectorMarksSameHostRouteChangesAsSpaTransitions() {
        NavigationEvent navigation = new NavigationEvent(
                "nav-1",
                "login",
                "https://app.test/login",
                "dashboard",
                "https://app.test/dashboard/index",
                "click:submit",
                true,
                "test"
        );

        var transitions = new SpaStateTransitionDetector().detect(List.of(navigation));

        Assert.assertEquals(transitions.size(), 1);
        Assert.assertEquals(transitions.get(0).transitionType(), "SPA_ROUTE_CHANGE");
        Assert.assertTrue(transitions.get(0).routeChanged());
        Assert.assertTrue(transitions.get(0).sameDocument());
    }

    @Test
    public void seleniumFallbackCollectorBuildsRuntimeEvidenceBundle() {
        SeleniumDiscoveryResult result = new SeleniumDiscoveryResult(
                "https://app.test",
                List.of(pageWithNetworkCall()),
                List.of(new DiscoveredTransition("login", "Login", "click", "dashboard", "https://app.test/dashboard/index", true))
        );

        RuntimeEvidenceBundle bundle = new SeleniumLogRuntimeEvidenceCollector().collect(result);

        Assert.assertEquals(bundle.networkResponses().size(), 1);
        Assert.assertEquals(bundle.semanticNetworkEvidence().size(), 1);
        Assert.assertEquals(bundle.stateTransitions().size(), 1);
        Assert.assertTrue(bundle.sourceTrace().stream().anyMatch(trace -> trace.contains("selenium-log-runtime-evidence")));
    }

    @Test
    public void runtimeMergerAddsSemanticNetworkRelationsToMatchingPage() {
        PageModel page = new PageModel(
                "login",
                "https://app.test/login",
                "/login",
                "Login",
                "Login",
                "authentication",
                new PageEvidenceModel("", ""),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        RuntimeEvidenceBundle bundle = new RuntimeEvidenceBundle(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new SemanticNetworkEvidence(
                        "network-evidence-1",
                        "login",
                        "https://app.test/login",
                        "POST",
                        "/api/login",
                        200,
                        "XHR",
                        "AUTHENTICATE",
                        "authentication",
                        0.95d,
                        "test"
                )),
                List.of(),
                List.of()
        );

        PageModelBundle merged = new RuntimeEvidencePageModelMerger().merge(new PageModelBundle(List.of(page)), bundle);

        Assert.assertEquals(merged.pages().get(0).apiRelations().size(), 1);
        Assert.assertEquals(merged.pages().get(0).apiRelations().get(0).endpoint(), "/api/login");
        Assert.assertEquals(merged.pages().get(0).apiRelations().get(0).relationType(), "authenticate");
        Assert.assertTrue(new PageModelMappedPageMapper().map(merged.pages().get(0)).actions().stream()
                .anyMatch(action -> action.actionType().equals("authenticate")
                        && action.description().contains("/api/login")));
    }

    @Test
    public void semanticGraphIncludesRuntimeNetworkEvidence() {
        PageModelBundle pages = new PageModelBundle(List.of(loginPageModel()));
        MappedUiKnowledge mapped = mappedKnowledge();
        RuntimeEvidenceBundle runtime = runtimeBundle();

        var semanticActions = new SemanticActionModelBuilder().build(pages, mapped);
        var graph = new SemanticGraphBuilder().build(pages, mapped, semanticActions, runtime);

        Assert.assertTrue(graph.nodes().stream()
                .anyMatch(node -> node.nodeType().equals("RuntimeNetworkEvidence")
                        && node.metadata().get("endpoint").equals("/api/login")));
        Assert.assertTrue(graph.edges().stream()
                .anyMatch(edge -> edge.edgeType().equals("SEMANTIC_PAGE_OBSERVED_RUNTIME_NETWORK")));
    }

    @Test
    public void semanticGraphEnricherAddsVectorDocumentsForPersistence() {
        MappedUiKnowledge enriched = new SemanticGraphMappedKnowledgeEnricher().enrich(
                new PageModelBundle(List.of(loginPageModel())),
                mappedKnowledge(),
                runtimeBundle()
        );

        Assert.assertTrue(enriched.graphNodes().stream()
                .anyMatch(node -> node.nodeType().equals("RuntimeNetworkEvidence")));
        Assert.assertTrue(enriched.vectorDocuments().stream()
                .anyMatch(document -> document.documentType().equals("semantic-graph-summary")
                        && document.text().contains("/api/login")));
    }

    @Test
    public void runtimeFeedbackFlagsNetworkFailuresForReview() {
        RuntimeEvidenceBundle runtime = new RuntimeEvidenceBundle(
                List.of(),
                List.of(response("login", "GET", "https://app.test/api/login", 500, "XHR")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        var summary = new RuntimeFeedbackAnalyzer().analyze(new PageModelBundle(List.of(loginPageModel())), runtime);

        Assert.assertEquals(summary.networkFailures(), 1);
        Assert.assertTrue(summary.issues().stream()
                .anyMatch(issue -> issue.issueType().equals("NETWORK_FAILURES")));
    }

    private NetworkResponseEvent response(String pageId, String method, String url, int status, String resourceType) {
        return new NetworkResponseEvent(
                "response-1",
                pageId,
                "https://app.test/" + pageId,
                method,
                url,
                new ua.demo.agentlab.ui.discovery.runtime.RuntimeEventNormalizer().path(url),
                status,
                resourceType,
                "2026-07-04T00:00:00Z",
                "test"
        );
    }

    private DiscoveredPageSnapshot pageWithNetworkCall() {
        RawPageSnapshot raw = new RawPageSnapshot(
                "https://app.test/login",
                "/login",
                "Login",
                "<html></html>",
                "<html></html>",
                "Login",
                "",
                "",
                "2026-07-04T00:00:00Z",
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(new BrowserNetworkCall("request-1", "POST", "https://app.test/api/login", 200, "XHR"))
        );
        return new DiscoveredPageSnapshot(
                "login",
                "https://app.test/login",
                "Login",
                List.of("Login"),
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                List.of("AUTHENTICATION"),
                List.of(),
                "fingerprint",
                new ua.demo.agentlab.ui.discovery.evidence.model.DiscoveredPageEvidence("", ""),
                raw,
                List.of()
        );
    }

    private PageModel loginPageModel() {
        return new PageModel(
                "login",
                "https://app.test/login",
                "/login",
                "Login",
                "Login",
                "authentication",
                new PageEvidenceModel("", ""),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private MappedUiKnowledge mappedKnowledge() {
        MappedPage page = new MappedPage(
                "login",
                "LoginPage",
                "authentication",
                "https://app.test/login",
                "/login",
                "Login",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new PageStateHints(true, false, false, false, true, false),
                "",
                ""
        );
        return new MappedUiKnowledge(
                List.of(page),
                List.of(),
                List.of(new ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode(
                        "login",
                        "Page",
                        "LoginPage",
                        "login",
                        Map.of("route", "/login")
                )),
                List.of(),
                List.of()
        );
    }

    private RuntimeEvidenceBundle runtimeBundle() {
        return new RuntimeEvidenceBundle(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new SemanticNetworkEvidence(
                        "network-evidence-1",
                        "login",
                        "https://app.test/login",
                        "POST",
                        "/api/login",
                        200,
                        "XHR",
                        "AUTHENTICATE",
                        "authentication",
                        0.95d,
                        "test"
                )),
                List.of(),
                List.of()
        );
    }
}
