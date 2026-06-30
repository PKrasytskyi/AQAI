package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.templates.DefaultProjectContextScanner;
import ua.demo.agentlab.templates.DefaultTemplateRegistry;
import ua.demo.agentlab.templates.ProjectContext;
import ua.demo.agentlab.templates.TemplateDescriptor;
import ua.demo.agentlab.ui.agent.LayeredUiTestWriterAgent;
import ua.demo.agentlab.ui.agent.PageObjectWriterAgent;
import ua.demo.agentlab.ui.selenium.writer.SeleniumTemplatePageObjectWriter;
import ua.demo.agentlab.ui.selenium.writer.SeleniumTemplateUiTestWriter;
import ua.demo.agentlab.ui.selenium.writer.TemplateDrivenSeleniumWriter;

import java.nio.file.Path;

public class TemplateModuleFactory {

    public TemplateModule create(ProjectProfile projectProfile, GenerationPolicy policy) {
        if (projectProfile == null || policy == null) {
            throw new IllegalArgumentException("projectProfile and policy are required for template module");
        }
        ProjectContext projectContext = new DefaultProjectContextScanner(Path.of("")).scan();
        TemplateDescriptor templateDescriptor = new DefaultTemplateRegistry().resolve(policy, projectContext);
        TemplateDrivenSeleniumWriter seleniumWriter = new TemplateDrivenSeleniumWriter(
                projectProfile.outputProfile().generatedPagesPackage(),
                projectProfile.outputProfile().generatedTestsPackage(),
                templateDescriptor.supportPackage()
        );
        return new TemplateModule(
                seleniumWriter,
                new PageObjectWriterAgent(new SeleniumTemplatePageObjectWriter(seleniumWriter)),
                new LayeredUiTestWriterAgent(new SeleniumTemplateUiTestWriter(seleniumWriter))
        );
    }
}
