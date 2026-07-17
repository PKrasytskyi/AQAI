package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.ai.artifactdiff.AiRunWorkingDirectoryArchiver;
import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.RuntimeProperties;
import ua.demo.agentlab.demo.DemoManifestLoader;
import ua.demo.agentlab.demo.DemoManifestPreflightService;
import ua.demo.agentlab.demo.DemoPreflightReport;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.model.RequirementInput;

import java.nio.file.Path;

public class AppBootstrap {

    private static final String DEFAULT_OBJECTIVE =
            "Generate canonical UI test cases from requirements for UI automation";

    private final WorkflowRequestFactory requestFactory;
    private final WorkflowCoreModuleFactory coreModuleFactory;
    private final DeterministicWorkflowFactory deterministicWorkflowFactory;
    private final AiPromptWorkflowFactory aiPromptWorkflowFactory;
    private final ApiDemoWorkflowFactory apiDemoWorkflowFactory;

    public AppBootstrap() {
        this(
                new WorkflowRequestFactory(),
                new WorkflowCoreModuleFactory(),
                new DeterministicWorkflowFactory(),
                new AiPromptWorkflowFactory(),
                new ApiDemoWorkflowFactory()
        );
    }

    AppBootstrap(
            WorkflowRequestFactory requestFactory,
            WorkflowCoreModuleFactory coreModuleFactory,
            DeterministicWorkflowFactory deterministicWorkflowFactory,
            AiPromptWorkflowFactory aiPromptWorkflowFactory,
            ApiDemoWorkflowFactory apiDemoWorkflowFactory
    ) {
        if (requestFactory == null
                || coreModuleFactory == null
                || deterministicWorkflowFactory == null
                || aiPromptWorkflowFactory == null
                || apiDemoWorkflowFactory == null) {
            throw new IllegalArgumentException("bootstrap collaborators cannot be null");
        }
        this.requestFactory = requestFactory;
        this.coreModuleFactory = coreModuleFactory;
        this.deterministicWorkflowFactory = deterministicWorkflowFactory;
        this.aiPromptWorkflowFactory = aiPromptWorkflowFactory;
        this.apiDemoWorkflowFactory = apiDemoWorkflowFactory;
    }

    public WorkflowDefinition createWorkflow(String[] args) {
        WorkflowRequest request = requestFactory.create(args);
        WorkflowState initialState = initialState(request);
        WorkflowCoreComponents core = coreModuleFactory.create(request.projectProfile());
        if (request.mode() == WorkflowMode.AI_PROMPT) {
            new AiRunWorkingDirectoryArchiver().archiveAndCleanBeforeRun();
            validateDemoManifest();
            return aiPromptWorkflowFactory.create(initialState, core);
        }
        if (request.mode() == WorkflowMode.API_DEMO) {
            return apiDemoWorkflowFactory.create(initialState, core);
        }
        return deterministicWorkflowFactory.create(initialState, core);
    }

    private void validateDemoManifest() {
        RuntimeProperties properties = new RuntimeProperties();
        if (!properties.readBoolean("demo.preflight.enabled", "false")) {
            return;
        }
        String configuredManifest = properties.readValue("demo.manifest.file", "");
        if (configuredManifest.isBlank()) {
            throw new IllegalStateException("Demo preflight is enabled but demo.manifest.file is not configured.");
        }
        var manifest = new DemoManifestLoader().load(Path.of(configuredManifest));
        DemoPreflightReport report = new DemoManifestPreflightService().validate(
                manifest, Path.of("").toAbsolutePath().normalize());
        new AiRunArtifactWriter().writeJson("quality", "demo-input-readiness.json", report);
        if (!report.ready()) {
            String details = report.issues().stream()
                    .map(issue -> issue.code() + ": " + issue.message())
                    .reduce((left, right) -> left + "; " + right)
                    .orElse("unknown demo preflight failure");
            throw new IllegalStateException("Demo preflight failed: " + details);
        }
    }

    private WorkflowState initialState(WorkflowRequest request) {
        WorkflowState state = new WorkflowState(
                DEFAULT_OBJECTIVE,
                new RequirementInput(request.sourceType(), request.requirementLocation())
        );
        ProjectProfile projectProfile = request.projectProfile();
        state.setProjectProfile(projectProfile);
        state.addArtifact("project.profile.id", projectProfile.profileId());
        state.addArtifact("project.profile.name", projectProfile.projectName());
        state.addArtifact("project.base.url", projectProfile.baseUrl());
        state.addArtifact("workflow.mode", request.mode().name());
        return state;
    }
}
