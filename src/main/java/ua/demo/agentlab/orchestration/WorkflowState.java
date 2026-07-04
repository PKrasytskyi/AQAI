package ua.demo.agentlab.orchestration;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheLookupResult;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeRaw;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedUiContractValidationResult;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowState {

    private final WorkflowRunEnvelope runEnvelope;
    private RequirementDocument requirementDocument;
    private TestPlan testPlan;
    private UiTestPlan uiTestPlan;
    private List<GeneratedSourceFile> pageObjectFiles = new ArrayList<>();
    private List<GeneratedSourceFile> uiTestFiles = new ArrayList<>();
    private List<GeneratedSourceFile> apiSourceFiles = new ArrayList<>();
    private List<GeneratedSourceFile> apiTestFiles = new ArrayList<>();
    private List<String> writtenFiles = new ArrayList<>();
    private GeneratedCodeValidationResult generatedCodeValidationResult;
    private GeneratedCodeReviewReport generatedCodeReviewReport;
    private NormalizedRequirementBundle normalizedRequirementBundle;
    private CanonicalTestCaseBundle canonicalTestCaseBundle;
    private GenerationPolicy generationPolicy;
    private ProjectProfile projectProfile;
    private UiDiscoverySnapshot uiDiscoverySnapshot;
    private MappedUiKnowledge mappedUiKnowledge;
    private MappedUiKnowledgeRaw mappedUiKnowledgeRaw;
    private MappedUiKnowledgeCurated mappedUiKnowledgeCurated;
    private SeleniumDiscoveryResult seleniumDiscoveryResult;
    private RuntimeEvidenceBundle runtimeEvidenceBundle;
    private PageModelBundle pageModelBundle;
    private MappedUiKnowledge enrichedMappedUiKnowledge;
    private PageKnowledgeCacheLookupResult pageKnowledgeCacheLookupResult;
    private List<PageModelEnrichmentRecord> pageModelEnrichments = new ArrayList<>();
    private List<AssertionContract> assertionContracts = new ArrayList<>();
    private KnowledgeRunMetadata knowledgeRunMetadata;
    private CanonicalPageFlowModel canonicalPageFlowModel;
    private FlowScopedKnowledgePackage flowScopedKnowledgePackage;
    private List<String> discoveryArtifactFiles = new ArrayList<>();
    private AiContextPackage aiContextPackage;
    private PromptUiEvidence promptUiEvidence;
    private List<AiPageObjectSpec> aiPageObjectSpecs = new ArrayList<>();
    private List<AiUiTestSpec> aiUiTestSpecs = new ArrayList<>();
    private List<String> aiArtifactFiles = new ArrayList<>();
    private GeneratedUiContractValidationResult generatedUiContractValidationResult;

    public WorkflowState(String objective, RequirementInput requirementInput) {
        this.runEnvelope = WorkflowRunEnvelope.create(objective, requirementInput);
    }

    public WorkflowRunEnvelope runEnvelope() {
        return runEnvelope;
    }

    public String getObjective() {
        return runEnvelope.objective();
    }

    public RequirementInput getRequirementInput() {
        return runEnvelope.requirementInput();
    }

    public RequirementDocument getRequirementDocument() {
        return requirementDocument;
    }

    public void setRequirementDocument(RequirementDocument requirementDocument) {
        this.requirementDocument = requirementDocument;
    }

    public GeneratedCodeReviewReport getGeneratedCodeReviewReport() {
        return generatedCodeReviewReport;
    }

    public void setGeneratedCodeReviewReport(GeneratedCodeReviewReport generatedCodeReviewReport) {
        this.generatedCodeReviewReport = generatedCodeReviewReport;
    }

    public TestPlan getTestPlan() {
        return testPlan;
    }

    public void setTestPlan(TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    public UiTestPlan getUiTestPlan(){
        return uiTestPlan;
    }

    public void setUiTestPlan(UiTestPlan uiTestPlan){
        this.uiTestPlan = uiTestPlan;
    }


    public List<GeneratedSourceFile> getPageObjectFiles()   {
        return pageObjectFiles;

    }

    public void setPageObjectFiles(List<GeneratedSourceFile> files) {
        this.pageObjectFiles = files;
    }

    public List<GeneratedSourceFile> getUiTestFiles() {
        return uiTestFiles;

    }
    public void setUiTestFiles(List<GeneratedSourceFile> files) {
        this.uiTestFiles = files;
    }

    public List<GeneratedSourceFile> getApiSourceFiles() {
        return apiSourceFiles;
    }

    public void setApiSourceFiles(List<GeneratedSourceFile> apiSourceFiles) {
        this.apiSourceFiles = apiSourceFiles == null ? new ArrayList<>() : new ArrayList<>(apiSourceFiles);
    }

    public List<GeneratedSourceFile> getApiTestFiles() {
        return apiTestFiles;
    }

    public void setApiTestFiles(List<GeneratedSourceFile> apiTestFiles) {
        this.apiTestFiles = apiTestFiles == null ? new ArrayList<>() : new ArrayList<>(apiTestFiles);
    }

    public List<String> getWrittenFiles() {
        return writtenFiles;
    }

    public void addWrittenFile(String path) {
        this.writtenFiles.add(path);
    }

    public Map<String, String> getArtifacts() {
        return runEnvelope.artifactRefs().asMap();
    }

    public List<String> getFindings() {
        return runEnvelope.findings().entries();
    }

    public List<String> getAuditTrail() {
        return runEnvelope.audit().entries();
    }

    public boolean isFailed() {
        return runEnvelope.runMetadata().failed();
    }

    public String getFailureReason() {
        return runEnvelope.runMetadata().failureReason();
    }

    public void addArtifact(String key, String value) {
        runEnvelope.artifactRefs().put(key, value);
    }

    public void addFinding(String finding) {
        runEnvelope.findings().add(finding);
    }

    public void addAudit(String message) {
        runEnvelope.audit().add(message);
    }

    public GeneratedCodeValidationResult getGeneratedCodeValidationResult() {
        return generatedCodeValidationResult;
    }

    public void setGeneratedCodeValidationResult(GeneratedCodeValidationResult generatedCodeValidationResult) {
        this.generatedCodeValidationResult = generatedCodeValidationResult;
    }

    public NormalizedRequirementBundle getNormalizedRequirementBundle() {
        return normalizedRequirementBundle;
    }

    public void setNormalizedRequirementBundle(NormalizedRequirementBundle normalizedRequirementBundle) {
        this.normalizedRequirementBundle = normalizedRequirementBundle;
    }

    public CanonicalTestCaseBundle getCanonicalTestCaseBundle() {
        return canonicalTestCaseBundle;
    }

    public void setCanonicalTestCaseBundle(CanonicalTestCaseBundle canonicalTestCaseBundle) {
        this.canonicalTestCaseBundle = canonicalTestCaseBundle;
    }

    public GenerationPolicy getGenerationPolicy() {
        return generationPolicy;
    }

    public void setGenerationPolicy(GenerationPolicy generationPolicy) {
        this.generationPolicy = generationPolicy;
    }

    public ProjectProfile getProjectProfile() {
        return projectProfile;
    }

    public void setProjectProfile(ProjectProfile projectProfile) {
        this.projectProfile = projectProfile;
    }

    public UiDiscoverySnapshot getUiDiscoverySnapshot() {
        return uiDiscoverySnapshot;
    }

    public void setUiDiscoverySnapshot(UiDiscoverySnapshot uiDiscoverySnapshot) {
        this.uiDiscoverySnapshot = uiDiscoverySnapshot;
    }

    public CanonicalPageFlowModel getCanonicalPageFlowModel() {
        return canonicalPageFlowModel;
    }

    public void setCanonicalPageFlowModel(CanonicalPageFlowModel canonicalPageFlowModel) {
        this.canonicalPageFlowModel = canonicalPageFlowModel;
    }

    public FlowScopedKnowledgePackage getFlowScopedKnowledgePackage() {
        return flowScopedKnowledgePackage;
    }

    public void setFlowScopedKnowledgePackage(FlowScopedKnowledgePackage flowScopedKnowledgePackage) {
        this.flowScopedKnowledgePackage = flowScopedKnowledgePackage;
    }

    public SeleniumDiscoveryResult getSeleniumDiscoveryResult() {
        return seleniumDiscoveryResult;
    }

    public void setSeleniumDiscoveryResult(SeleniumDiscoveryResult seleniumDiscoveryResult) {
        this.seleniumDiscoveryResult = seleniumDiscoveryResult;
    }

    public RuntimeEvidenceBundle getRuntimeEvidenceBundle() {
        return runtimeEvidenceBundle;
    }

    public void setRuntimeEvidenceBundle(RuntimeEvidenceBundle runtimeEvidenceBundle) {
        this.runtimeEvidenceBundle = runtimeEvidenceBundle;
    }

    public PageModelBundle getPageModelBundle() {
        return pageModelBundle;
    }

    public void setPageModelBundle(PageModelBundle pageModelBundle) {
        this.pageModelBundle = pageModelBundle;
    }

    public MappedUiKnowledge getEnrichedMappedUiKnowledge() {
        return enrichedMappedUiKnowledge;
    }

    public void setEnrichedMappedUiKnowledge(MappedUiKnowledge enrichedMappedUiKnowledge) {
        this.enrichedMappedUiKnowledge = enrichedMappedUiKnowledge;
    }

    public PageKnowledgeCacheLookupResult getPageKnowledgeCacheLookupResult() {
        return pageKnowledgeCacheLookupResult;
    }

    public void setPageKnowledgeCacheLookupResult(PageKnowledgeCacheLookupResult pageKnowledgeCacheLookupResult) {
        this.pageKnowledgeCacheLookupResult = pageKnowledgeCacheLookupResult;
    }

    public List<PageModelEnrichmentRecord> getPageModelEnrichments() {
        return pageModelEnrichments;
    }

    public void setPageModelEnrichments(List<PageModelEnrichmentRecord> pageModelEnrichments) {
        this.pageModelEnrichments = pageModelEnrichments == null ? new ArrayList<>() : new ArrayList<>(pageModelEnrichments);
    }

    public List<AssertionContract> getAssertionContracts() {
        return assertionContracts;
    }

    public void setAssertionContracts(List<AssertionContract> assertionContracts) {
        this.assertionContracts = assertionContracts == null ? new ArrayList<>() : new ArrayList<>(assertionContracts);
    }

    public KnowledgeRunMetadata getKnowledgeRunMetadata() {
        return knowledgeRunMetadata;
    }

    public void setKnowledgeRunMetadata(KnowledgeRunMetadata knowledgeRunMetadata) {
        this.knowledgeRunMetadata = knowledgeRunMetadata;
    }

    public MappedUiKnowledge getMappedUiKnowledge() {
        return mappedUiKnowledge;
    }

    public void setMappedUiKnowledge(MappedUiKnowledge mappedUiKnowledge) {
        this.mappedUiKnowledge = mappedUiKnowledge;
    }

    public MappedUiKnowledgeRaw getMappedUiKnowledgeRaw() {
        return mappedUiKnowledgeRaw;
    }

    public void setMappedUiKnowledgeRaw(MappedUiKnowledgeRaw mappedUiKnowledgeRaw) {
        this.mappedUiKnowledgeRaw = mappedUiKnowledgeRaw;
    }

    public MappedUiKnowledgeCurated getMappedUiKnowledgeCurated() {
        return mappedUiKnowledgeCurated;
    }

    public void setMappedUiKnowledgeCurated(MappedUiKnowledgeCurated mappedUiKnowledgeCurated) {
        this.mappedUiKnowledgeCurated = mappedUiKnowledgeCurated;
    }

    public PromptUiEvidence getPromptUiEvidence() {
        return promptUiEvidence;
    }

    public void setPromptUiEvidence(PromptUiEvidence promptUiEvidence) {
        this.promptUiEvidence = promptUiEvidence;
    }

    public List<String> getDiscoveryArtifactFiles() {
        return discoveryArtifactFiles;
    }

    public void addDiscoveryArtifactFile(String path) {
        this.discoveryArtifactFiles.add(path);
    }

    public AiContextPackage getAiContextPackage() {
        return aiContextPackage;
    }

    public void setAiContextPackage(AiContextPackage aiContextPackage) {
        this.aiContextPackage = aiContextPackage;
    }

    public List<AiPageObjectSpec> getAiPageObjectSpecs() {
        return aiPageObjectSpecs;
    }

    public void setAiPageObjectSpecs(List<AiPageObjectSpec> aiPageObjectSpecs) {
        this.aiPageObjectSpecs = aiPageObjectSpecs == null ? new ArrayList<>() : new ArrayList<>(aiPageObjectSpecs);
    }

    public List<AiUiTestSpec> getAiUiTestSpecs() {
        return aiUiTestSpecs;
    }

    public void setAiUiTestSpecs(List<AiUiTestSpec> aiUiTestSpecs) {
        this.aiUiTestSpecs = aiUiTestSpecs == null ? new ArrayList<>() : new ArrayList<>(aiUiTestSpecs);
    }

    public List<String> getAiArtifactFiles() {
        return aiArtifactFiles;
    }

    public void addAiArtifactFile(String path) {
        this.aiArtifactFiles.add(path);
    }

    public GeneratedUiContractValidationResult getGeneratedUiContractValidationResult() {
        return generatedUiContractValidationResult;
    }

    public void setGeneratedUiContractValidationResult(
            GeneratedUiContractValidationResult generatedUiContractValidationResult
    ) {
        this.generatedUiContractValidationResult = generatedUiContractValidationResult;
    }

    public void fail(String reason) {
        runEnvelope.runMetadata().fail(reason);
    }
}
