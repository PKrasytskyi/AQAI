package ua.demo.agentlab.api.discovery;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiEndpointDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiSpecification;
import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.HttpMethod;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserNetworkCall;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.List;
import java.util.Map;

public class ApiEndpointDiscoveryAdapterTest {

    @Test
    public void endpointSeedParserBuildsStableEndpointEvidence() {
        ApiEndpointBundle bundle = new ApiEndpointSeedParser().parse(
                "test-seed",
                "GET /public/v2/users listUsers; POST /public/v2/users createUser"
        );

        Assert.assertEquals(bundle.endpoints().size(), 2);
        Assert.assertTrue(bundle.find("GET:/public/v2/users").orElseThrow().confirmed());
        Assert.assertEquals(bundle.find("POST:/public/v2/users").orElseThrow().method(), HttpMethod.POST);
        Assert.assertEquals(bundle.find("POST:/public/v2/users").orElseThrow().responses().get(0).statusCode(), 201);
    }

    @Test
    public void openApiAdapterConvertsOpenApiSpecificationsIntoEndpointBundle() {
        OpenApiSpecification specification = new OpenApiSpecification(
                "openapi.json",
                "Demo API",
                List.of(new OpenApiEndpointDefinition(
                        "openapi.json",
                        "GET",
                        "/public/v2/users",
                        "listUsers",
                        "List users"
                ))
        );

        ApiEndpointBundle bundle = new OpenApiEndpointAdapter().adapt(List.of(specification));

        Assert.assertEquals(bundle.endpoints().size(), 1);
        Assert.assertTrue(bundle.find("GET:/public/v2/users").orElseThrow().confirmed());
    }

    @Test
    public void networkAdapterKeepsSameOriginApiCallsOnly() {
        SeleniumDiscoveryResult result = new SeleniumDiscoveryResult(
                "https://gorest.co.in",
                List.of(pageWithNetworkCalls()),
                List.of()
        );

        ApiEndpointBundle bundle = new NetworkEndpointAdapter().adapt(result);

        Assert.assertTrue(bundle.find("GET:/public/v2/users").isPresent());
        Assert.assertTrue(bundle.find("POST:/public/v2/users").isPresent());
        Assert.assertTrue(bundle.find("GET:/assets/app.js").isEmpty());
        Assert.assertTrue(bundle.find("GET:/track").isEmpty());
    }

    private DiscoveredPageSnapshot pageWithNetworkCalls() {
        RawPageSnapshot raw = new RawPageSnapshot(
                "https://gorest.co.in",
                "/",
                "GoREST",
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(
                        new BrowserNetworkCall("1", "GET", "https://gorest.co.in/public/v2/users", 200, "XHR"),
                        new BrowserNetworkCall("2", "POST", "https://gorest.co.in/public/v2/users", 201, "Fetch"),
                        new BrowserNetworkCall("3", "GET", "https://gorest.co.in/assets/app.js", 200, "Script"),
                        new BrowserNetworkCall("4", "GET", "https://analytics.example.test/track", 200, "XHR")
                )
        );
        return new DiscoveredPageSnapshot(
                "home",
                "https://gorest.co.in",
                "GoREST",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                List.of(),
                List.of(),
                "fingerprint",
                null,
                raw,
                List.of()
        );
    }
}
