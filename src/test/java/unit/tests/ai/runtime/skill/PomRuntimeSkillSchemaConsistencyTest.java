package unit.tests.ai.runtime.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.runtime.skill.RuntimeSkillPromptLoader;

import java.io.InputStream;

public class PomRuntimeSkillSchemaConsistencyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void runtimePomSkillUsesTheSameSchemaAsThePlatformValidator() throws Exception {
        JsonNode runtimeSchema = objectMapper.readTree(
                new RuntimeSkillPromptLoader().load("pom-json-generation").outputSchema());
        try (InputStream input = getClass().getResourceAsStream("/schemas/pom-contract-v1.schema.json")) {
            Assert.assertNotNull(input, "Platform POM contract schema must be packaged");
            JsonNode platformSchema = objectMapper.readTree(input);
            Assert.assertEquals(runtimeSchema, platformSchema,
                    "Runtime skill schema must remain identical to the platform POM contract schema");
        }
    }
}
