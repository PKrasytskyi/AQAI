package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.review.RuleBasedGeneratedCodeReviewer;
import ua.demo.agentlab.review.agent.GeneratedCodeReviewAgent;
import ua.demo.agentlab.validation.GeneratedCodeValidator;
import ua.demo.agentlab.validation.MavenGeneratedCodeValidator;
import ua.demo.agentlab.validation.SimpleGeneratedUiContractValidator;
import ua.demo.agentlab.validation.agent.GeneratedCodeCompileAgent;
import ua.demo.agentlab.validation.agent.GeneratedUiContractValidationAgent;

import java.nio.file.Path;

public class ValidationModuleFactory {

    public ValidationModule create() {
        GeneratedCodeValidator generatedCodeValidator =
                new MavenGeneratedCodeValidator(Path.of("").toAbsolutePath().normalize().toString());
        return new ValidationModule(
                generatedCodeValidator,
                new GeneratedUiContractValidationAgent(new SimpleGeneratedUiContractValidator()),
                new GeneratedCodeCompileAgent(generatedCodeValidator),
                new GeneratedCodeReviewAgent(new RuleBasedGeneratedCodeReviewer())
        );
    }
}
