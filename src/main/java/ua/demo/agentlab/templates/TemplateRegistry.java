package ua.demo.agentlab.templates;

import ua.demo.agentlab.policy.model.GenerationPolicy;

public interface TemplateRegistry {

    TemplateDescriptor resolve(GenerationPolicy policy, ProjectContext projectContext);
}
