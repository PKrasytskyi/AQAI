package unit.tests.ai.debug;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.debug.ArtifactSecretRedactor;

import java.util.Map;

public class ArtifactSecretRedactorTest {

    @Test
    public void redactsResolvedRuntimeDataButKeepsOrdinaryEvidence() {
        JsonNode result = new ArtifactSecretRedactor().redact(Map.of(
                "resolvedData", Map.of("username", "demo-user", "password", "demo-password"),
                "elementName", "passwordInput",
                "route", "/auth/login"
        ), new ObjectMapper());

        Assert.assertEquals(result.path("resolvedData").path("username").asText(), "${REDACTED}");
        Assert.assertEquals(result.path("resolvedData").path("password").asText(), "${REDACTED}");
        Assert.assertEquals(result.path("elementName").asText(), "passwordInput");
        Assert.assertEquals(result.path("route").asText(), "/auth/login");
    }
}
