package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.config.ProjectProfile;

public class WorkflowCoreModuleFactory {

    private final RequirementPolicyModuleFactory requirementPolicyModuleFactory;
    private final TemplateModuleFactory templateModuleFactory;
    private final PersistenceModuleFactory persistenceModuleFactory;
    private final DiscoveryModuleFactory discoveryModuleFactory;
    private final KnowledgeStoreModuleFactory knowledgeStoreModuleFactory;
    private final TestPlanningModuleFactory testPlanningModuleFactory;
    private final ValidationModuleFactory validationModuleFactory;
    private final ApiModuleFactory apiModuleFactory;

    public WorkflowCoreModuleFactory() {
        this(
                new RequirementPolicyModuleFactory(),
                new TemplateModuleFactory(),
                new PersistenceModuleFactory(),
                new DiscoveryModuleFactory(),
                new KnowledgeStoreModuleFactory(),
                new TestPlanningModuleFactory(),
                new ValidationModuleFactory(),
                new ApiModuleFactory()
        );
    }

    WorkflowCoreModuleFactory(
            RequirementPolicyModuleFactory requirementPolicyModuleFactory,
            TemplateModuleFactory templateModuleFactory,
            PersistenceModuleFactory persistenceModuleFactory,
            DiscoveryModuleFactory discoveryModuleFactory,
            KnowledgeStoreModuleFactory knowledgeStoreModuleFactory,
            TestPlanningModuleFactory testPlanningModuleFactory,
            ValidationModuleFactory validationModuleFactory,
            ApiModuleFactory apiModuleFactory
    ) {
        if (requirementPolicyModuleFactory == null
                || templateModuleFactory == null
                || persistenceModuleFactory == null
                || discoveryModuleFactory == null
                || knowledgeStoreModuleFactory == null
                || testPlanningModuleFactory == null
                || validationModuleFactory == null
                || apiModuleFactory == null) {
            throw new IllegalArgumentException("core module factories cannot be null");
        }
        this.requirementPolicyModuleFactory = requirementPolicyModuleFactory;
        this.templateModuleFactory = templateModuleFactory;
        this.persistenceModuleFactory = persistenceModuleFactory;
        this.discoveryModuleFactory = discoveryModuleFactory;
        this.knowledgeStoreModuleFactory = knowledgeStoreModuleFactory;
        this.testPlanningModuleFactory = testPlanningModuleFactory;
        this.validationModuleFactory = validationModuleFactory;
        this.apiModuleFactory = apiModuleFactory;
    }

    public WorkflowCoreComponents create(ProjectProfile projectProfile) {
        if (projectProfile == null) {
            throw new IllegalArgumentException("projectProfile cannot be null");
        }

        RequirementPolicyModule requirementPolicy = requirementPolicyModuleFactory.create();
        TemplateModule template = templateModuleFactory.create(projectProfile, requirementPolicy.defaultPolicy());
        PersistenceModule persistence = persistenceModuleFactory.create();
        DiscoveryModule discovery = discoveryModuleFactory.create(projectProfile);
        KnowledgeStoreModule knowledgeStore = knowledgeStoreModuleFactory.create();
        TestPlanningModule testPlanning = testPlanningModuleFactory.create();
        ValidationModule validation = validationModuleFactory.create();
        ApiModule api = apiModuleFactory.create(persistence.generatedFileWriter());

        return new WorkflowCoreComponents(
                template.seleniumWriter(),
                persistence.generatedFileWriter(),
                validation.generatedCodeValidator(),
                knowledgeStore.canonicalInteractionLayer(),
                requirementPolicy.requirementReaderAgent(),
                requirementPolicy.requirementNormalizationAgent(),
                requirementPolicy.policyLoadingAgent(),
                discovery.uiDiscoveryAgent(),
                discovery.uiRuntimeEvidenceAgent(),
                discovery.uiPageModelAgent(),
                discovery.uiPageMappingAgent(),
                knowledgeStore.uiPageKnowledgePersistenceAgent(),
                discovery.uiDiscoveryArtifactPersistenceAgent(),
                api.apiGenerationAgent(),
                api.apiGeneratedSourcePersistenceAgent(),
                knowledgeStore.flowScopedKnowledgeAgent(),
                testPlanning.requirementToTestCaseAgent(),
                testPlanning.uiTestPlanAgent(),
                template.pageObjectWriterAgent(),
                template.layeredUiTestWriterAgent(),
                persistence.filePersistenceAgent(),
                validation.generatedUiContractValidationAgent(),
                validation.generatedCodeCompileAgent(),
                validation.generatedCodeReviewAgent(),
                validation.generatedUiSmokeAgent(),
                validation.runtimeFeedbackDbUpdateAgent()
        );
    }
}
