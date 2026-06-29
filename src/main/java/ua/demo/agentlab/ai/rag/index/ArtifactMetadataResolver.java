package ua.demo.agentlab.ai.rag.index;

import ua.demo.agentlab.ai.rag.model.ArtifactType;
import ua.demo.agentlab.ai.rag.model.IndexedArtifact;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtifactMetadataResolver {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(class|interface|enum|record)\\s+([A-Za-z0-9_]+)");

    public IndexedArtifact classify(SourceDocument document) {
        String normalizedPath = document.relativePath().replace('\\', '/');
        String lowerPath = normalizedPath.toLowerCase(Locale.ROOT);
        String content = document.content();
        ArtifactType artifactType = detectArtifactType(lowerPath, content);
        String artifactName = detectArtifactName(document, content);
        String packageName = detectPackageName(content);
        List<String> tags = detectTags(lowerPath, content, artifactType);
        return new IndexedArtifact(
                document.path(),
                document.relativePath(),
                document.language(),
                artifactType,
                artifactName,
                packageName,
                tags
        );
    }

    private ArtifactType detectArtifactType(String lowerPath, String content) {
        if (lowerPath.endsWith(".feature")) {
            return ArtifactType.FEATURE_FILE;
        }
        if (lowerPath.endsWith(".md")) {
            if (lowerPath.contains("policy") || lowerPath.contains("rule") || lowerPath.contains("locator")) {
                return ArtifactType.POLICY;
            }
            return ArtifactType.DOCUMENTATION;
        }
        if (lowerPath.endsWith(".properties") || lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml")) {
            return ArtifactType.CONFIGURATION;
        }
        if (lowerPath.contains("/generated/tests/") || lowerPath.contains("/tests/") || lowerPath.endsWith("test.java")) {
            return ArtifactType.TEST_CLASS;
        }
        if (lowerPath.contains("apiclient") || lowerPath.endsWith("client.java") || lowerPath.contains("/api/")) {
            return ArtifactType.API_CLIENT;
        }
        if (lowerPath.contains("/generated/pages/") || lowerPath.endsWith("page.java")) {
            return ArtifactType.PAGE_OBJECT;
        }
        if (lowerPath.contains("/data/") || lowerPath.endsWith("testdataprovider.java")
                || lowerPath.endsWith("factory.java") || lowerPath.contains("credential")) {
            return ArtifactType.TEST_DATA;
        }
        if (lowerPath.endsWith("basepage.java") || lowerPath.endsWith("basetest.java")) {
            return ArtifactType.BASE_CLASS;
        }
        if (lowerPath.contains("/policy/")) {
            return ArtifactType.POLICY;
        }
        if (lowerPath.contains("/config/")) {
            return ArtifactType.CONFIGURATION;
        }
        if (content.contains("@Test") || content.contains("@BeforeMethod")) {
            return ArtifactType.TEST_CLASS;
        }
        return ArtifactType.UTILITY;
    }

    private String detectArtifactName(SourceDocument document, String content) {
        Matcher classMatcher = CLASS_PATTERN.matcher(content);
        if (classMatcher.find()) {
            return classMatcher.group(2);
        }
        String fileName = Path.of(document.relativePath()).getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    private String detectPackageName(String content) {
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }

    private List<String> detectTags(String lowerPath, String content, ArtifactType artifactType) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(artifactType.name().toLowerCase(Locale.ROOT));
        addIfContains(tags, lowerPath, "login");
        addIfContains(tags, lowerPath, "auth");
        addIfContains(tags, lowerPath, "credential");
        addIfContains(tags, lowerPath, "locator");
        addIfContains(tags, lowerPath, "policy");
        addIfContains(tags, lowerPath, "page");
        addIfContains(tags, lowerPath, "test");
        addIfContains(tags, lowerPath, "api");
        addIfContains(tags, lowerPath, "session");

        String normalizedContent = content.toLowerCase(Locale.ROOT);
        addIfContains(tags, normalizedContent, "login");
        addIfContains(tags, normalizedContent, "password");
        addIfContains(tags, normalizedContent, "username");
        addIfContains(tags, normalizedContent, "negative");
        addIfContains(tags, normalizedContent, "invalid");
        addIfContains(tags, normalizedContent, "selector");
        addIfContains(tags, normalizedContent, "auth");
        addIfContains(tags, normalizedContent, "token");
        addIfContains(tags, normalizedContent, "session");

        return List.copyOf(tags);
    }

    private void addIfContains(Set<String> tags, String text, String candidate) {
        if (text.contains(candidate)) {
            tags.add(candidate);
        }
    }
}
