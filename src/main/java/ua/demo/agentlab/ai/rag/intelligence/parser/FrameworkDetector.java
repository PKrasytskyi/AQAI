package ua.demo.agentlab.ai.rag.intelligence.parser;

import ua.demo.agentlab.ai.rag.intelligence.model.FrameworkDetectionResult;
import ua.demo.agentlab.ai.rag.intelligence.model.FrameworkType;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FrameworkDetector {

    public FrameworkDetectionResult detect(List<SourceDocument> documents) {
        Set<FrameworkType> frameworks = new LinkedHashSet<>();
        Map<FrameworkType, String> evidence = new LinkedHashMap<>();

        for (SourceDocument document : documents) {
            String path = document.relativePath().toLowerCase(Locale.ROOT);
            String content = document.content().toLowerCase(Locale.ROOT);
            if ("pom.xml".equalsIgnoreCase(document.relativePath())) {
                frameworks.add(FrameworkType.MAVEN);
                evidence.putIfAbsent(FrameworkType.MAVEN, document.relativePath());
            }
            if (content.contains("spring-boot-starter") || content.contains("@springbootapplication")
                    || content.contains("@restcontroller")) {
                frameworks.add(FrameworkType.SPRING_BOOT);
                evidence.putIfAbsent(FrameworkType.SPRING_BOOT, document.relativePath());
            }
            if (content.contains("org.selenium") || content.contains("webdriver")) {
                frameworks.add(FrameworkType.SELENIUM);
                evidence.putIfAbsent(FrameworkType.SELENIUM, document.relativePath());
            }
            if (content.contains("org.testng") || content.contains("@test")) {
                frameworks.add(FrameworkType.TESTNG);
                evidence.putIfAbsent(FrameworkType.TESTNG, document.relativePath());
            }
            if (content.contains("io.cucumber") || path.endsWith(".feature")) {
                frameworks.add(FrameworkType.CUCUMBER);
                evidence.putIfAbsent(FrameworkType.CUCUMBER, document.relativePath());
            }
            if (content.contains("rest-assured") || content.contains("io.restassured")) {
                frameworks.add(FrameworkType.REST_ASSURED);
                evidence.putIfAbsent(FrameworkType.REST_ASSURED, document.relativePath());
            }
            if (content.contains("openapi:") || content.contains("\"openapi\"") || content.contains("\"swagger\"")
                    || path.contains("swagger") || path.contains("openapi")) {
                frameworks.add(FrameworkType.OPENAPI);
                evidence.putIfAbsent(FrameworkType.OPENAPI, document.relativePath());
            }
        }

        if (frameworks.isEmpty()) {
            frameworks.add(FrameworkType.UNKNOWN);
            evidence.put(FrameworkType.UNKNOWN, "No known framework markers found");
        }

        return new FrameworkDetectionResult(frameworks, evidence);
    }
}
