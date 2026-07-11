package unit.tests.ai.runtime.skill;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.runtime.skill.RuntimeSkillPrompt;
import ua.demo.agentlab.ai.runtime.skill.RuntimeSkillPromptLoader;

public class RuntimeSkillPromptLoaderTest {

    @Test
    public void loaderReadsRuntimeSkillPromptRulesAndSchemas() {
        RuntimeSkillPrompt skill = new RuntimeSkillPromptLoader().load("pom-json-generation");

        Assert.assertEquals(skill.skillId(), "pom-json-generation");
        Assert.assertEquals(skill.version(), "1.0.0");
        Assert.assertTrue(skill.prompt().contains("Page Object Contract Planner"));
        Assert.assertTrue(skill.rules().contains("Do not write Java"));
        Assert.assertTrue(skill.inputSchema().contains("pom-json-generation-input.v1"));
        Assert.assertTrue(skill.outputSchema().contains("pom-contract-v1"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void loaderRejectsUnsafeSkillIds() {
        new RuntimeSkillPromptLoader().load("../pom-json-generation");
    }
}

