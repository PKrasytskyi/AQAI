package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.EndpointMatch;
import ua.demo.agentlab.ai.rag.intelligence.model.ConfidenceAssessment;
import ua.demo.agentlab.ai.rag.intelligence.model.FrameworkType;
import ua.demo.agentlab.ai.rag.intelligence.model.IncrementalIndexStats;
import ua.demo.agentlab.ai.rag.intelligence.model.IndexingWarning;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiSpecification;
import ua.demo.agentlab.ai.rag.intelligence.model.ParserMetric;
import ua.demo.agentlab.ai.rag.intelligence.model.ParsingDiagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RepositoryKnowledgeValidator {

    public ParsingDiagnostics buildDiagnostics(
            List<ua.demo.agentlab.ai.rag.model.SourceDocument> documents,
            ua.demo.agentlab.ai.rag.intelligence.model.FrameworkDetectionResult frameworkDetection,
            List<ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition> controllerRoutes,
            List<OpenApiSpecification> openApiSpecifications,
            List<EndpointMatch> endpointMatches,
            List<ua.demo.agentlab.ai.rag.intelligence.model.InitialTestIdea> initialTestIdeas,
            IncrementalIndexStats incrementalIndexStats,
            List<JavaAstParseResult> javaAstResults,
            List<ParserMetric> parserMetrics
    ) {
        int javaFiles = countLanguage(documents, "java");
        int yamlFiles = countLanguage(documents, "yaml");
        int jsonFiles = countLanguage(documents, "json");
        int markdownFiles = countLanguage(documents, "markdown");
        int featureFiles = countLanguage(documents, "gherkin");
        int xmlFiles = countLanguage(documents, "xml");

        List<IndexingWarning> warnings = new ArrayList<>();
        boolean springDetected = frameworkDetection.frameworks().contains(FrameworkType.SPRING_BOOT);
        boolean openApiDetected = frameworkDetection.frameworks().contains(FrameworkType.OPENAPI);
        long astFailures = javaAstResults.stream().filter(result -> !result.parsed()).count();

        if (springDetected && controllerRoutes.isEmpty()) {
            warnings.add(new IndexingWarning(
                    "SPRING_WITHOUT_CONTROLLERS",
                    "Spring Boot markers were detected but no controller routes were parsed."
            ));
        }
        if (openApiDetected && openApiSpecifications.stream().flatMap(spec -> spec.endpoints().stream()).findAny().isEmpty()) {
            warnings.add(new IndexingWarning(
                    "OPENAPI_WITHOUT_ENDPOINTS",
                    "OpenAPI markers were detected but no endpoints were parsed."
            ));
        }
        long unmatchedEndpoints = endpointMatches.stream().filter(match -> !match.matched()).count();
        if (unmatchedEndpoints > 0) {
            warnings.add(new IndexingWarning(
                    "UNMATCHED_ENDPOINTS",
                    "Some controller routes did not match OpenAPI endpoints: " + unmatchedEndpoints
            ));
        }
        if (controllerRoutes.size() > 0 && initialTestIdeas.isEmpty()) {
            warnings.add(new IndexingWarning(
                    "NO_INITIAL_TEST_IDEAS",
                    "Controller routes were found but no initial test ideas were generated."
            ));
        }
        boolean likelyApiProject = documents.stream()
                .anyMatch(document -> document.relativePath().toLowerCase(Locale.ROOT).contains("/api/")
                        || document.content().contains("@RestController"));
        if (likelyApiProject && !openApiDetected && openApiSpecifications.isEmpty()) {
            warnings.add(new IndexingWarning(
                    "API_WITHOUT_OPENAPI",
                    "API-related code was detected but no OpenAPI specification was found."
            ));
        }
        if (astFailures > 0) {
            warnings.add(new IndexingWarning(
                    "JAVA_AST_PARSE_FAILURES",
                    "Some Java files could not be parsed via AST parser: " + astFailures
            ));
        }
        if (incrementalIndexStats.reusedPreviousSnapshot() && incrementalIndexStats.unchangedDocuments() == 0) {
            warnings.add(new IndexingWarning(
                    "INCREMENTAL_REUSE_MISSED",
                    "Incremental indexing was enabled but no unchanged documents were reused."
            ));
        }

        List<ConfidenceAssessment> confidenceAssessments = new ArrayList<>();
        confidenceAssessments.add(new ConfidenceAssessment(
                "java-ast-parse",
                javaAstResults.isEmpty()
                        ? 0.0d
                        : javaAstResults.stream().mapToDouble(JavaAstParseResult::confidenceScore).average().orElse(0.0d),
                "Average confidence across parsed Java compilation units."
        ));
        confidenceAssessments.add(new ConfidenceAssessment(
                "openapi-route-alignment",
                controllerRoutes.isEmpty()
                        ? 1.0d
                        : endpointMatches.stream().filter(EndpointMatch::matched).count() / (double) controllerRoutes.size(),
                "Ratio of controller routes that align with discovered OpenAPI contracts."
        ));
        confidenceAssessments.add(new ConfidenceAssessment(
                "incremental-reuse",
                documents.isEmpty()
                        ? 1.0d
                        : incrementalIndexStats.unchangedDocuments() / (double) Math.max(1, documents.size()),
                "Share of current documents reused from the previous snapshot."
        ));
        confidenceAssessments.add(new ConfidenceAssessment(
                "test-idea-signal",
                controllerRoutes.isEmpty()
                        ? 1.0d
                        : Math.min(1.0d, initialTestIdeas.size() / (double) controllerRoutes.size()),
                "Relative richness of generated initial ideas versus discovered routes."
        ));

        return new ParsingDiagnostics(
                documents.size(),
                javaFiles,
                yamlFiles,
                jsonFiles,
                markdownFiles,
                featureFiles,
                xmlFiles,
                incrementalIndexStats,
                parserMetrics,
                confidenceAssessments,
                warnings
        );
    }

    private int countLanguage(List<ua.demo.agentlab.ai.rag.model.SourceDocument> documents, String language) {
        return (int) documents.stream().filter(document -> language.equalsIgnoreCase(document.language())).count();
    }
}
