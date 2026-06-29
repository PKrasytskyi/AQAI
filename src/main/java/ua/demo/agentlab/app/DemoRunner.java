package ua.demo.agentlab.app;

import ua.demo.agentlab.ai.context.AiContextAssembler;
import ua.demo.agentlab.ai.context.RuleBasedCanonicalInteractionLayer;
import ua.demo.agentlab.ai.context.UiKnowledgeRetrievalService;
import ua.demo.agentlab.ai.artifactdiff.AiRunWorkingDirectoryArchiver;
import ua.demo.agentlab.ai.assertions.agent.AssertionContractAgent;
import ua.demo.agentlab.ai.expectationenrichment.agent.TestCaseExpectationEnrichmentAgent;
import ua.demo.agentlab.ai.expectationenrichment.service.OpenAiTestCaseExpectationEnrichmentClient;
import ua.demo.agentlab.ai.expectationenrichment.service.RuleBasedTestCaseExpectationEnrichmentClient;
import ua.demo.agentlab.ai.expectationenrichment.service.TestCaseExpectationEnrichmentClient;
import ua.demo.agentlab.ai.flow.BusinessFlowResolver;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeAgent;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeRefreshAgent;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeService;
import ua.demo.agentlab.ai.openai.PropertiesOpenAiRuntimeConfig;
import ua.demo.agentlab.ai.pageenrichment.agent.PageModelEnrichmentAgent;
import ua.demo.agentlab.ai.pageenrichment.agent.PageKnowledgeCacheLookupAgent;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheQueryService;
import ua.demo.agentlab.ai.pageenrichment.service.OpenAiPageModelEnrichmentClient;
import ua.demo.agentlab.ai.pageenrichment.service.PageModelEnrichmentClient;
import ua.demo.agentlab.ai.pageenrichment.service.RuleBasedPageModelEnrichmentClient;
import ua.demo.agentlab.ai.rag.config.PropertiesRagRuntimeConfig;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.ui.agent.AiContextAssemblyAgent;
import ua.demo.agentlab.ai.ui.agent.AiPageObjectSpecAgent;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectSpecGenerator;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.ProjectProfileLoader;
import ua.demo.agentlab.config.PropertiesProjectProfileLoader;
import ua.demo.agentlab.orchestration.AgentOrchestrator;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.persistence.GeneratedFileWriter;
import ua.demo.agentlab.persistence.LocalFilePersistenceAgent;
import ua.demo.agentlab.persistence.LocalGeneratedFileWriter;
import ua.demo.agentlab.policy.PolicyLoadingAgent;
import ua.demo.agentlab.policy.PolicyResolver;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.policy.provider.DefaultGenerationPolicyProvider;
import ua.demo.agentlab.requirements.agent.RequirementReaderAgent;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.normalization.RuleBasedRequirementNormalizer;
import ua.demo.agentlab.requirements.normalization.agent.RequirementNormalizationAgent;
import ua.demo.agentlab.requirements.source.FileRequirementSource;
import ua.demo.agentlab.requirements.source.RequirementSource;
import ua.demo.agentlab.requirements.source.UrlRequirementSource;
import ua.demo.agentlab.reporting.ConsoleReportPrinter;
import ua.demo.agentlab.review.RuleBasedGeneratedCodeReviewer;
import ua.demo.agentlab.review.agent.GeneratedCodeReviewAgent;
import ua.demo.agentlab.templates.DefaultProjectContextScanner;
import ua.demo.agentlab.templates.DefaultTemplateRegistry;
import ua.demo.agentlab.templates.ProjectContext;
import ua.demo.agentlab.templates.TemplateDescriptor;
import ua.demo.agentlab.testcase.agent.RequirementToTestCaseAgent;
import ua.demo.agentlab.testcase.generator.RuleBasedRequirementToTestCaseGenerator;
import ua.demo.agentlab.ui.agent.LayeredUiTestWriterAgent;
import ua.demo.agentlab.ui.agent.PageObjectWriterAgent;
import ua.demo.agentlab.ui.agent.UiTestPlanAgent;
import ua.demo.agentlab.ui.discovery.classification.PageClassificationService;
import ua.demo.agentlab.ui.discovery.classification.RuleBasedPageClassificationService;
import ua.demo.agentlab.ui.discovery.CompositeUiDiscoveryService;
import ua.demo.agentlab.ui.discovery.RuleBasedUiDiscoveryService;
import ua.demo.agentlab.ui.discovery.agent.UiDiscoveryArtifactPersistenceAgent;
import ua.demo.agentlab.ui.discovery.agent.UiDiscoveryAgent;
import ua.demo.agentlab.ui.discovery.agent.UiPageMappingAgent;
import ua.demo.agentlab.ui.discovery.agent.UiPageKnowledgePersistenceAgent;
import ua.demo.agentlab.ui.discovery.agent.UiPageModelAgent;
import ua.demo.agentlab.ui.discovery.evidence.LocalPageEvidenceCaptureService;
import ua.demo.agentlab.ui.discovery.mapping.RuleBasedPageMapper;
import ua.demo.agentlab.ui.discovery.enrichment.UiDiscoveryEnricher;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelArtifactWriter;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelBuilder;
import ua.demo.agentlab.ui.discovery.policy.DiscoveryCrawlPolicy;
import ua.demo.agentlab.ui.discovery.persistence.LocalDiscoveryArtifactWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.GraphPageKnowledgeWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.QdrantPageKnowledgeWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesKnowledgeVectorRuntimeConfig;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.selenium.SeleniumUiDiscoveryService;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationConfig;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationService;
import ua.demo.agentlab.ui.discovery.selenium.collector.PageSnapshotCollector;
import ua.demo.agentlab.ui.discovery.selenium.crawler.SafeNavigationCrawler;
import ua.demo.agentlab.ui.discovery.selenium.extractor.FormStructureExtractor;
import ua.demo.agentlab.ui.discovery.selenium.extractor.InteractiveElementExtractor;
import ua.demo.agentlab.ui.flow.RuleBasedCanonicalPageFlowMapper;
import ua.demo.agentlab.ui.generator.CanonicalTestCaseUiPlanGenerator;
import ua.demo.agentlab.core.config.PropertiesUiRuntimeConfig;
import ua.demo.agentlab.core.config.UiRuntimeConfig;
import ua.demo.agentlab.core.ui.driver.DefaultDriverFactory;
import ua.demo.agentlab.core.ui.driver.DriverFactory;
import ua.demo.agentlab.ui.selenium.writer.SeleniumTemplatePageObjectWriter;
import ua.demo.agentlab.ui.selenium.writer.SeleniumTemplateUiTestWriter;
import ua.demo.agentlab.ui.selenium.writer.TemplateDrivenSeleniumWriter;
import ua.demo.agentlab.validation.GeneratedCodeValidator;
import ua.demo.agentlab.validation.MavenGeneratedCodeValidator;
import ua.demo.agentlab.validation.SimpleGeneratedUiContractValidator;
import ua.demo.agentlab.validation.agent.GeneratedCodeCompileAgent;
import ua.demo.agentlab.validation.agent.GeneratedUiContractValidationAgent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class DemoRunner {

    private static final String DEFAULT_REQUIREMENT_LOCATION = "requirements/valid-author.md";
    private static final ConsoleReportPrinter REPORT_PRINTER = new ConsoleReportPrinter();

    public static void main(String[] args) {
        String requirementLocation = resolveRequirementLocation(args);
        SourceType sourceType = detectSourceType(requirementLocation);

        WorkflowState workflowState = new WorkflowState(
                "Generate canonical UI test cases from requirements for UI automation",
                new RequirementInput(sourceType, requirementLocation)
        );
        ProjectProfileLoader projectProfileLoader = new PropertiesProjectProfileLoader();
        ProjectProfile projectProfile = projectProfileLoader.loadDefaultProfile();
        workflowState.setProjectProfile(projectProfile);
        workflowState.addArtifact("project.profile.id", projectProfile.profileId());
        workflowState.addArtifact("project.profile.name", projectProfile.projectName());
        workflowState.addArtifact("project.base.url", projectProfile.baseUrl());

        RequirementSource fileSource = new FileRequirementSource();
        RequirementSource urlSource = new UrlRequirementSource();

        WorkflowAgent requirementReaderAgent = new RequirementReaderAgent(List.of(fileSource, urlSource));
        WorkflowAgent requirementNormalizationAgent =
                new RequirementNormalizationAgent(new RuleBasedRequirementNormalizer());

        DefaultGenerationPolicyProvider defaultGenerationPolicyProvider = new DefaultGenerationPolicyProvider();
        PolicyResolver policyResolver = new PolicyResolver(List.of(defaultGenerationPolicyProvider));
        WorkflowAgent policyLoadingAgent = new PolicyLoadingAgent(policyResolver);
        GenerationPolicy defaultPolicy = defaultGenerationPolicyProvider.provide(
                defaultGenerationPolicyProvider.defaultPolicyId()
        );
        ProjectContext projectContext = new DefaultProjectContextScanner(Path.of("")).scan();
        TemplateDescriptor templateDescriptor = new DefaultTemplateRegistry().resolve(defaultPolicy, projectContext);
        TemplateDrivenSeleniumWriter seleniumWriter = new TemplateDrivenSeleniumWriter(
                projectProfile.outputProfile().generatedPagesPackage(),
                projectProfile.outputProfile().generatedTestsPackage(),
                templateDescriptor.supportPackage()
        );
        UiRuntimeConfig runtimeConfig = new PropertiesUiRuntimeConfig();
        DriverFactory discoveryDriverFactory = new DefaultDriverFactory(runtimeConfig);
        InteractiveElementExtractor interactiveElementExtractor = new InteractiveElementExtractor();
        FormStructureExtractor formStructureExtractor = new FormStructureExtractor(interactiveElementExtractor);
        PageSnapshotCollector pageSnapshotCollector = new PageSnapshotCollector(
                interactiveElementExtractor,
                formStructureExtractor
        );
        DiscoveryCrawlPolicy discoveryCrawlPolicy = DiscoveryCrawlPolicy.defaultPolicy(projectProfile);
        SafeNavigationCrawler safeNavigationCrawler = new SafeNavigationCrawler(
                pageSnapshotCollector,
                discoveryCrawlPolicy,
                new LocalPageEvidenceCaptureService(),
                new DiscoveryAuthenticationService(new DiscoveryAuthenticationConfig())
        );
        PageClassificationService pageClassificationService = new RuleBasedPageClassificationService();
        UiDiscoveryEnricher uiDiscoveryEnricher = new UiDiscoveryEnricher(pageClassificationService);
        SeleniumUiDiscoveryService seleniumUiDiscoveryService = new SeleniumUiDiscoveryService(
                discoveryDriverFactory,
                discoveryCrawlPolicy,
                safeNavigationCrawler,
                pageClassificationService
        );

        GeneratedFileWriter generatedFileWriter = new LocalGeneratedFileWriter();
        GeneratedCodeValidator generatedCodeValidator =
                new MavenGeneratedCodeValidator(
                        Path.of("").toAbsolutePath().normalize().toString());

        WorkflowAgent pageObjectWriterAgent = new PageObjectWriterAgent(
                new SeleniumTemplatePageObjectWriter(seleniumWriter)
        );
        WorkflowAgent layeredUiTestWriterAgent = new LayeredUiTestWriterAgent(
                new SeleniumTemplateUiTestWriter(seleniumWriter)
        );
        WorkflowAgent filePersistenceAgent = new LocalFilePersistenceAgent(generatedFileWriter);
        WorkflowAgent uiDiscoveryAgent = new UiDiscoveryAgent(
                new CompositeUiDiscoveryService(
                        new RuleBasedUiDiscoveryService(),
                        seleniumUiDiscoveryService,
                        uiDiscoveryEnricher
                ),
                new RuleBasedCanonicalPageFlowMapper()
        );
        WorkflowAgent uiDiscoveryArtifactPersistenceAgent =
                new UiDiscoveryArtifactPersistenceAgent(new LocalDiscoveryArtifactWriter());
        WorkflowAgent uiPageModelAgent = new UiPageModelAgent(
                new PageModelBuilder(),
                new PageModelArtifactWriter()
        );
        WorkflowAgent uiPageMappingAgent = new UiPageMappingAgent(new RuleBasedPageMapper());
        WorkflowAgent uiPageKnowledgePersistenceAgent = new UiPageKnowledgePersistenceAgent(List.of(
                new GraphPageKnowledgeWriter(new PropertiesNeo4jRuntimeConfig()),
                new QdrantPageKnowledgeWriter(new PropertiesKnowledgeVectorRuntimeConfig())
        ));
        RuleBasedCanonicalInteractionLayer canonicalInteractionLayer = new RuleBasedCanonicalInteractionLayer();
        WorkflowAgent flowScopedKnowledgeAgent = new FlowScopedKnowledgeAgent(
                new FlowScopedKnowledgeService(
                        new BusinessFlowResolver(),
                        canonicalInteractionLayer,
                        null
                )
        );
        WorkflowAgent requirementToTestCaseAgent = new RequirementToTestCaseAgent(
                new RuleBasedRequirementToTestCaseGenerator()
        );
        WorkflowAgent uiTestPlanAgent = new UiTestPlanAgent(new CanonicalTestCaseUiPlanGenerator());
        WorkflowAgent generatedUiContractValidationAgent =
                new GeneratedUiContractValidationAgent(new SimpleGeneratedUiContractValidator());
        WorkflowAgent generatedCodeCompileAgent = new GeneratedCodeCompileAgent(generatedCodeValidator);
        WorkflowAgent generatedCodeReviewAgent = new GeneratedCodeReviewAgent(new RuleBasedGeneratedCodeReviewer());

        if (shouldUseAi(args)) {
            new AiRunWorkingDirectoryArchiver().archiveAndCleanBeforeRun();
            PropertiesOpenAiRuntimeConfig openAiRuntimeConfig = new PropertiesOpenAiRuntimeConfig();
            RagRuntimeConfig ragRuntimeConfig = new PropertiesRagRuntimeConfig();
            PageModelEnrichmentClient pageModelEnrichmentClient = ragRuntimeConfig.enabled()
                    && ragRuntimeConfig.openAiApiKey() != null
                    && !ragRuntimeConfig.openAiApiKey().isBlank()
                    ? new OpenAiPageModelEnrichmentClient(ragRuntimeConfig)
                    : new RuleBasedPageModelEnrichmentClient();
            TestCaseExpectationEnrichmentClient testCaseExpectationEnrichmentClient = ragRuntimeConfig.enabled()
                    && ragRuntimeConfig.openAiApiKey() != null
                    && !ragRuntimeConfig.openAiApiKey().isBlank()
                    ? new OpenAiTestCaseExpectationEnrichmentClient(ragRuntimeConfig)
                    : new RuleBasedTestCaseExpectationEnrichmentClient();
            UiKnowledgeRetrievalService uiKnowledgeRetrievalService = new UiKnowledgeRetrievalService(
                    new PropertiesKnowledgeVectorRuntimeConfig(),
                    new PropertiesNeo4jRuntimeConfig(),
                    openAiRuntimeConfig
            );
            AiContextAssembler aiContextAssembler = new AiContextAssembler(
                    canonicalInteractionLayer,
                    uiKnowledgeRetrievalService
            );
            WorkflowAgent flowScopedKnowledgeAgentAi = new FlowScopedKnowledgeAgent(
                    new FlowScopedKnowledgeService(
                            new BusinessFlowResolver(),
                            canonicalInteractionLayer,
                            uiKnowledgeRetrievalService
                    )
            );
            WorkflowAgent testCaseExpectationEnrichmentAgent = new TestCaseExpectationEnrichmentAgent(
                    testCaseExpectationEnrichmentClient
            );
            WorkflowAgent assertionContractAgent = new AssertionContractAgent();
            WorkflowAgent pageKnowledgeCacheLookupAgent = new PageKnowledgeCacheLookupAgent(
                    new PageKnowledgeCacheQueryService(new PropertiesNeo4jRuntimeConfig())
            );
            WorkflowAgent pageModelEnrichmentAgent = new PageModelEnrichmentAgent(pageModelEnrichmentClient);
            WorkflowAgent flowScopedKnowledgeRefreshAgent = new FlowScopedKnowledgeRefreshAgent(
                    new FlowScopedKnowledgeService(
                            new BusinessFlowResolver(),
                            canonicalInteractionLayer,
                            uiKnowledgeRetrievalService
                    )
            );
            WorkflowAgent aiContextAssemblyAgent = new AiContextAssemblyAgent(aiContextAssembler);
            WorkflowAgent aiPageObjectSpecAgent = new AiPageObjectSpecAgent(
                    new AiPageObjectSpecGenerator(openAiRuntimeConfig, aiContextAssembler),
                    currentState -> seleniumWriter.buildAiBaselinePageObjectSpecs(currentState.getUiTestPlan())
            );
            List<WorkflowAgent> agents = List.of(
                    requirementReaderAgent,
                    requirementNormalizationAgent,
                    policyLoadingAgent,
                    uiDiscoveryAgent,
                    uiPageModelAgent,
                    uiPageMappingAgent,
                    uiPageKnowledgePersistenceAgent,
                    uiDiscoveryArtifactPersistenceAgent,
                    flowScopedKnowledgeAgentAi,
                    requirementToTestCaseAgent,
                    testCaseExpectationEnrichmentAgent,
                    assertionContractAgent,
                    uiTestPlanAgent,
                    pageKnowledgeCacheLookupAgent,
                    pageModelEnrichmentAgent,
                    flowScopedKnowledgeRefreshAgent,
                    aiContextAssemblyAgent,
                    aiPageObjectSpecAgent
            );

            runWorkflow(
                    workflowState,
                    agents
            );
            return;
        }
        List<WorkflowAgent> agents = List.of(
                requirementReaderAgent,
                requirementNormalizationAgent,
                policyLoadingAgent,
                uiDiscoveryAgent,
                uiPageModelAgent,
                uiPageMappingAgent,
                uiPageKnowledgePersistenceAgent,
                uiDiscoveryArtifactPersistenceAgent,
                flowScopedKnowledgeAgent,
                requirementToTestCaseAgent,
                uiTestPlanAgent,
                pageObjectWriterAgent,
                layeredUiTestWriterAgent,
                filePersistenceAgent,
                generatedUiContractValidationAgent,
                generatedCodeCompileAgent,
                generatedCodeReviewAgent
        );

        runWorkflow(
                workflowState,
                agents
        );
    }

    private static void runWorkflow(WorkflowState workflowState, List<WorkflowAgent> agents) {
        AgentOrchestrator orchestrator = new AgentOrchestrator(agents);
        WorkflowState result = orchestrator.run(workflowState);

        if (result.isFailed()) {
            REPORT_PRINTER.printWorkflowFailure(result);
            return;
        }

        REPORT_PRINTER.printWorkflowSummary(result);
    }

    private static boolean shouldUseAi(String[] args) {
        if (Arrays.stream(args).anyMatch("--ai"::equalsIgnoreCase)) {
            return true;
        }
        PropertiesOpenAiRuntimeConfig config = new PropertiesOpenAiRuntimeConfig();
        return config.enabled() && config.apiKey() != null && !config.apiKey().isBlank();
    }

    private static String resolveRequirementLocation(String[] args) {
        String requestedLocation = Arrays.stream(args)
                .filter(arg -> !arg.startsWith("--"))
                .findFirst()
                .orElse(DEFAULT_REQUIREMENT_LOCATION);

        if (isMissingFileLocation(requestedLocation) && Files.exists(Path.of(DEFAULT_REQUIREMENT_LOCATION))) {
            System.err.println("Requirement file not found: " + requestedLocation
                    + ". Falling back to " + DEFAULT_REQUIREMENT_LOCATION + ".");
            return DEFAULT_REQUIREMENT_LOCATION;
        }

        return requestedLocation;
    }

    private static boolean isMissingFileLocation(String location) {
        if (location == null || location.isBlank()) {
            return false;
        }
        if (location.startsWith("http://") || location.startsWith("https://")) {
            return false;
        }
        return !Files.exists(Path.of(location));
    }

    private static SourceType detectSourceType(String location) {
        return location.startsWith("http://") || location.startsWith("https://")
                ? SourceType.URL
                : SourceType.FILE;
    }
}
