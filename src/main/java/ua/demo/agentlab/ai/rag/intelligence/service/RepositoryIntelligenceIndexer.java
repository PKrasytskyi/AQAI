package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.graph.CodeRelationshipExtractor;
import ua.demo.agentlab.ai.rag.graph.ProjectCodeGraph;
import ua.demo.agentlab.ai.rag.index.ArtifactMetadataResolver;
import ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.DocumentFingerprint;
import ua.demo.agentlab.ai.rag.intelligence.model.DtoModelDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.EndpointMatch;
import ua.demo.agentlab.ai.rag.intelligence.model.ExistingTestDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.FrameworkDetectionResult;
import ua.demo.agentlab.ai.rag.intelligence.model.IncrementalIndexStats;
import ua.demo.agentlab.ai.rag.intelligence.model.InitialTestIdea;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRecord;
import ua.demo.agentlab.ai.rag.intelligence.model.LayerComponentDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiSpecification;
import ua.demo.agentlab.ai.rag.intelligence.model.ParserMetric;
import ua.demo.agentlab.ai.rag.intelligence.model.ParsingDiagnostics;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphEntity;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphRelation;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryIntelligenceReport;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryKnowledgeSnapshot;
import ua.demo.agentlab.ai.rag.intelligence.model.SourceInventoryItem;
import ua.demo.agentlab.ai.rag.intelligence.model.VectorSummary;
import ua.demo.agentlab.ai.rag.intelligence.parser.ControllerRouteParser;
import ua.demo.agentlab.ai.rag.intelligence.parser.DtoModelParser;
import ua.demo.agentlab.ai.rag.intelligence.parser.EndpointMatcher;
import ua.demo.agentlab.ai.rag.intelligence.parser.ExistingTestParser;
import ua.demo.agentlab.ai.rag.intelligence.parser.FrameworkDetector;
import ua.demo.agentlab.ai.rag.intelligence.parser.JavaAstParser;
import ua.demo.agentlab.ai.rag.intelligence.parser.OpenApiParser;
import ua.demo.agentlab.ai.rag.intelligence.parser.ServiceRepositoryParser;
import ua.demo.agentlab.ai.rag.intelligence.store.GraphStore;
import ua.demo.agentlab.ai.rag.intelligence.store.KnowledgeStore;
import ua.demo.agentlab.ai.rag.intelligence.store.LocalPersistentGraphStore;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RepositoryIntelligenceIndexer {

    private final RepositorySourceScanner repositorySourceScanner;
    private final DocumentFingerprintService documentFingerprintService;
    private final FrameworkDetector frameworkDetector;
    private final JavaAstParser javaAstParser;
    private final ControllerRouteParser controllerRouteParser;
    private final DtoModelParser dtoModelParser;
    private final ServiceRepositoryParser serviceRepositoryParser;
    private final ExistingTestParser existingTestParser;
    private final OpenApiParser openApiParser;
    private final EndpointMatcher endpointMatcher;
    private final KnowledgeGraphBuilder knowledgeGraphBuilder;
    private final VectorSummaryFactory vectorSummaryFactory;
    private final InitialTestIdeaGenerator initialTestIdeaGenerator;
    private final RepositoryKnowledgeValidator repositoryKnowledgeValidator;
    private final KnowledgeEnrichmentService knowledgeEnrichmentService;
    private final KnowledgeStore knowledgeStore;
    private final GraphStore graphStore;
    private final VectorSummaryIndexer vectorSummaryIndexer;
    private final RepositoryIntelligenceConfig config;

    public RepositoryIntelligenceIndexer(KnowledgeStore knowledgeStore) {
        this(
                RepositoryIntelligenceConfig.defaults(Path.of("target/repository-intelligence")),
                knowledgeStore,
                new LocalPersistentGraphStore(),
                new DisabledVectorSummaryIndexer()
        );
    }

    public RepositoryIntelligenceIndexer(
            RepositoryIntelligenceConfig config,
            KnowledgeStore knowledgeStore,
            GraphStore graphStore,
            VectorSummaryIndexer vectorSummaryIndexer
    ) {
        this(config, knowledgeStore, graphStore, vectorSummaryIndexer, new KnowledgeEnrichmentService());
    }

    public RepositoryIntelligenceIndexer(
            RepositoryIntelligenceConfig config,
            KnowledgeStore knowledgeStore,
            GraphStore graphStore,
            VectorSummaryIndexer vectorSummaryIndexer,
            KnowledgeEnrichmentService knowledgeEnrichmentService
    ) {
        this.config = config;
        this.repositorySourceScanner = new RepositorySourceScanner(config);
        this.documentFingerprintService = new DocumentFingerprintService();
        this.frameworkDetector = new FrameworkDetector();
        this.javaAstParser = new JavaAstParser();
        this.controllerRouteParser = new ControllerRouteParser();
        this.dtoModelParser = new DtoModelParser();
        this.serviceRepositoryParser = new ServiceRepositoryParser();
        this.existingTestParser = new ExistingTestParser();
        this.openApiParser = new OpenApiParser();
        this.endpointMatcher = new EndpointMatcher();
        this.knowledgeGraphBuilder = new KnowledgeGraphBuilder();
        this.vectorSummaryFactory = new VectorSummaryFactory();
        this.initialTestIdeaGenerator = new InitialTestIdeaGenerator();
        this.repositoryKnowledgeValidator = new RepositoryKnowledgeValidator();
        this.knowledgeEnrichmentService = knowledgeEnrichmentService == null
                ? new KnowledgeEnrichmentService()
                : knowledgeEnrichmentService;
        this.knowledgeStore = knowledgeStore;
        this.graphStore = graphStore;
        this.vectorSummaryIndexer = vectorSummaryIndexer;
    }

    public RepositoryIntelligenceReport index(Path workspaceRoot, Path outputDirectory) {
        List<SourceDocument> documents = repositorySourceScanner.scan(workspaceRoot);
        List<SourceInventoryItem> inventory = documents.stream()
                .map(document -> new SourceInventoryItem(document.relativePath(), document.language(), document.content().length()))
                .sorted(Comparator.comparing(SourceInventoryItem::relativePath))
                .toList();
        List<DocumentFingerprint> currentFingerprints = documentFingerprintService.build(documents);
        Optional<RepositoryKnowledgeSnapshot> previousSnapshotOptional = config.enableIncrementalIndexing()
                ? knowledgeStore.load(outputDirectory)
                : Optional.empty();
        RepositoryKnowledgeSnapshot previousSnapshot = previousSnapshotOptional.orElse(null);
        IncrementalIndexStats incrementalIndexStats = buildIncrementalStats(currentFingerprints, previousSnapshot);
        Set<String> changedPaths = determineChangedPaths(currentFingerprints, previousSnapshot);
        List<SourceDocument> changedDocuments = previousSnapshot == null
                ? documents
                : documents.stream().filter(document -> changedPaths.contains(document.relativePath())).toList();

        FrameworkDetectionResult frameworkDetection = frameworkDetector.detect(documents);
        List<JavaAstParseResult> parsedAstResults = javaAstParser.parse(changedDocuments);
        List<JavaAstParseResult> javaAstResults = mergeByPath(
                previousSnapshot == null ? List.of() : previousSnapshot.javaAstResults(),
                parsedAstResults,
                currentFingerprints.stream().map(DocumentFingerprint::relativePath).collect(Collectors.toSet()),
                JavaAstParseResult::relativePath
        );
        List<ControllerRouteDefinition> controllerRoutes = mergeByPath(
                previousSnapshot == null ? List.of() : previousSnapshot.controllerRoutes(),
                controllerRouteParser.parse(changedDocuments, parsedAstResults),
                currentFingerprints.stream().map(DocumentFingerprint::relativePath).collect(Collectors.toSet()),
                ControllerRouteDefinition::relativePath
        );
        List<DtoModelDefinition> dtoModels = mergeByPath(
                previousSnapshot == null ? List.of() : previousSnapshot.dtoModels(),
                dtoModelParser.parse(changedDocuments, parsedAstResults),
                currentFingerprints.stream().map(DocumentFingerprint::relativePath).collect(Collectors.toSet()),
                DtoModelDefinition::relativePath
        );
        List<LayerComponentDefinition> layerComponents = mergeByPath(
                previousSnapshot == null ? List.of() : previousSnapshot.layerComponents(),
                serviceRepositoryParser.parse(changedDocuments, parsedAstResults),
                currentFingerprints.stream().map(DocumentFingerprint::relativePath).collect(Collectors.toSet()),
                LayerComponentDefinition::relativePath
        );
        List<ExistingTestDefinition> existingTests = mergeByPath(
                previousSnapshot == null ? List.of() : previousSnapshot.existingTests(),
                existingTestParser.parse(changedDocuments, parsedAstResults),
                currentFingerprints.stream().map(DocumentFingerprint::relativePath).collect(Collectors.toSet()),
                ExistingTestDefinition::relativePath
        );
        List<OpenApiSpecification> openApiSpecifications = mergeByPath(
                previousSnapshot == null ? List.of() : previousSnapshot.openApiSpecifications(),
                openApiParser.parse(changedDocuments),
                currentFingerprints.stream().map(DocumentFingerprint::relativePath).collect(Collectors.toSet()),
                OpenApiSpecification::sourcePath
        );
        List<EndpointMatch> endpointMatches = endpointMatcher.match(controllerRoutes, openApiSpecifications);

        ProjectCodeGraph projectCodeGraph = new CodeRelationshipExtractor(
                new ua.demo.agentlab.ai.rag.source.WorkspaceDocumentCollector(),
                new ArtifactMetadataResolver()
        ).extract(workspaceRoot);

        List<InitialTestIdea> initialTestIdeas = initialTestIdeaGenerator.generate(
                controllerRoutes,
                endpointMatches,
                existingTests,
                openApiSpecifications
        );
        List<ParserMetric> parserMetrics = buildParserMetrics(
                documents,
                changedDocuments,
                javaAstResults,
                controllerRoutes,
                dtoModels,
                layerComponents,
                existingTests,
                openApiSpecifications,
                incrementalIndexStats
        );
        ParsingDiagnostics parsingDiagnostics = repositoryKnowledgeValidator.buildDiagnostics(
                documents,
                frameworkDetection,
                controllerRoutes,
                openApiSpecifications,
                endpointMatches,
                initialTestIdeas,
                incrementalIndexStats,
                javaAstResults,
                parserMetrics
        );
        List<KnowledgeEnrichmentRecord> knowledgeEnrichments = knowledgeEnrichmentService.enrich(
                documents,
                javaAstResults,
                layerComponents,
                existingTests,
                controllerRoutes
        );
        List<RepositoryGraphEntity> graphEntities = knowledgeGraphBuilder.buildEntities(
                frameworkDetection,
                projectCodeGraph,
                controllerRoutes,
                endpointMatches,
                dtoModels,
                layerComponents,
                existingTests,
                knowledgeEnrichments
        );
        List<RepositoryGraphRelation> graphRelations = knowledgeGraphBuilder.buildRelations(
                frameworkDetection,
                projectCodeGraph,
                controllerRoutes,
                endpointMatches,
                knowledgeEnrichments
        );
        List<VectorSummary> vectorSummaries = vectorSummaryFactory.build(
                frameworkDetection,
                controllerRoutes,
                dtoModels,
                layerComponents,
                existingTests,
                openApiSpecifications,
                initialTestIdeas,
                knowledgeEnrichments
        );
        graphStore.store(graphEntities, graphRelations, outputDirectory);
        int indexedVectorSummaries;
        try {
            indexedVectorSummaries = vectorSummaryIndexer.index(workspaceRoot, vectorSummaries);
        } catch (Exception exception) {
            indexedVectorSummaries = 0;
        }

        RepositoryKnowledgeSnapshot snapshot = new RepositoryKnowledgeSnapshot(
                frameworkDetection,
                parsingDiagnostics,
                inventory,
                currentFingerprints,
                javaAstResults,
                controllerRoutes,
                dtoModels,
                layerComponents,
                existingTests,
                openApiSpecifications,
                endpointMatches,
                graphEntities,
                graphRelations,
                knowledgeEnrichments,
                vectorSummaries,
                initialTestIdeas
        );
        Path storedOutput = knowledgeStore.store(snapshot, outputDirectory);
        return new RepositoryIntelligenceReport(
                documents.size(),
                parsingDiagnostics.warnings().size(),
                incrementalIndexStats.changedDocuments(),
                incrementalIndexStats.unchangedDocuments(),
                controllerRoutes.size(),
                dtoModels.size(),
                layerComponents.size(),
                existingTests.size(),
                openApiSpecifications.stream().mapToInt(spec -> spec.endpoints().size()).sum(),
                (int) endpointMatches.stream().filter(EndpointMatch::matched).count(),
                graphEntities.size(),
                graphRelations.size(),
                knowledgeEnrichments.size(),
                vectorSummaries.size(),
                initialTestIdeas.size(),
                indexedVectorSummaries,
                storedOutput
        );
    }

    private IncrementalIndexStats buildIncrementalStats(
            List<DocumentFingerprint> currentFingerprints,
            RepositoryKnowledgeSnapshot previousSnapshot
    ) {
        if (previousSnapshot == null || previousSnapshot.documentFingerprints().isEmpty()) {
            return new IncrementalIndexStats(false, currentFingerprints.size(), 0, currentFingerprints.size(), 0);
        }
        Map<String, DocumentFingerprint> previousByPath = previousSnapshot.documentFingerprints().stream()
                .collect(Collectors.toMap(DocumentFingerprint::relativePath, Function.identity(), (left, right) -> right));
        int unchanged = 0;
        int changed = 0;
        int added = 0;
        for (DocumentFingerprint current : currentFingerprints) {
            DocumentFingerprint previous = previousByPath.get(current.relativePath());
            if (previous == null) {
                changed++;
                added++;
                continue;
            }
            if (previous.checksum().equals(current.checksum())) {
                unchanged++;
            } else {
                changed++;
            }
        }
        Set<String> currentPaths = currentFingerprints.stream()
                .map(DocumentFingerprint::relativePath)
                .collect(Collectors.toSet());
        int removed = (int) previousByPath.keySet().stream().filter(path -> !currentPaths.contains(path)).count();
        return new IncrementalIndexStats(true, changed, unchanged, added, removed);
    }

    private Set<String> determineChangedPaths(
            List<DocumentFingerprint> currentFingerprints,
            RepositoryKnowledgeSnapshot previousSnapshot
    ) {
        if (previousSnapshot == null || previousSnapshot.documentFingerprints().isEmpty()) {
            return currentFingerprints.stream().map(DocumentFingerprint::relativePath).collect(Collectors.toSet());
        }
        Map<String, DocumentFingerprint> previousByPath = previousSnapshot.documentFingerprints().stream()
                .collect(Collectors.toMap(DocumentFingerprint::relativePath, Function.identity(), (left, right) -> right));
        return currentFingerprints.stream()
                .filter(current -> {
                    DocumentFingerprint previous = previousByPath.get(current.relativePath());
                    return previous == null || !previous.checksum().equals(current.checksum());
                })
                .map(DocumentFingerprint::relativePath)
                .collect(Collectors.toSet());
    }

    private <T> List<T> mergeByPath(
            List<T> previousValues,
            List<T> changedValues,
            Set<String> currentPaths,
            Function<T, String> pathExtractor
    ) {
        Map<String, List<T>> merged = new HashMap<>();
        for (T previousValue : previousValues) {
            String path = pathExtractor.apply(previousValue);
            if (path != null && currentPaths.contains(path)) {
                merged.computeIfAbsent(path, ignored -> new ArrayList<>()).add(previousValue);
            }
        }
        Map<String, List<T>> changedByPath = changedValues.stream()
                .collect(Collectors.groupingBy(pathExtractor));
        for (Map.Entry<String, List<T>> entry : changedByPath.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            merged.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return merged.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }

    private List<ParserMetric> buildParserMetrics(
            List<SourceDocument> documents,
            List<SourceDocument> changedDocuments,
            List<JavaAstParseResult> javaAstResults,
            List<ControllerRouteDefinition> controllerRoutes,
            List<DtoModelDefinition> dtoModels,
            List<LayerComponentDefinition> layerComponents,
            List<ExistingTestDefinition> existingTests,
            List<OpenApiSpecification> openApiSpecifications,
            IncrementalIndexStats incrementalIndexStats
    ) {
        int javaDocuments = (int) documents.stream().filter(document -> "java".equalsIgnoreCase(document.language())).count();
        int changedJavaDocuments = (int) changedDocuments.stream().filter(document -> "java".equalsIgnoreCase(document.language())).count();
        int reusedArtifacts = incrementalIndexStats.unchangedDocuments();
        return List.of(
                new ParserMetric(
                        "java-ast",
                        javaDocuments,
                        changedJavaDocuments,
                        javaAstResults.size(),
                        reusedArtifacts,
                        javaAstResults.stream().mapToDouble(JavaAstParseResult::confidenceScore).average().orElse(0.0d)
                ),
                new ParserMetric(
                        "controller-route",
                        javaDocuments,
                        javaDocuments,
                        controllerRoutes.size(),
                        reusedArtifacts,
                        controllerRoutes.isEmpty() ? 0.0d : 0.92d
                ),
                new ParserMetric(
                        "dto-model",
                        javaDocuments,
                        javaDocuments,
                        dtoModels.size(),
                        reusedArtifacts,
                        dtoModels.isEmpty() ? 0.0d : 0.88d
                ),
                new ParserMetric(
                        "service-repository",
                        javaDocuments,
                        javaDocuments,
                        layerComponents.size(),
                        reusedArtifacts,
                        layerComponents.isEmpty() ? 0.0d : 0.87d
                ),
                new ParserMetric(
                        "existing-test",
                        javaDocuments,
                        javaDocuments,
                        existingTests.size(),
                        reusedArtifacts,
                        existingTests.isEmpty() ? 0.0d : 0.9d
                ),
                new ParserMetric(
                        "openapi",
                        documents.size(),
                        documents.size(),
                        openApiSpecifications.stream().mapToInt(spec -> spec.endpoints().size()).sum(),
                        reusedArtifacts,
                        openApiSpecifications.isEmpty() ? 0.0d : 0.91d
                )
        );
    }
}
