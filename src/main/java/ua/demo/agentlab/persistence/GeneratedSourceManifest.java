package ua.demo.agentlab.persistence;

import ua.demo.agentlab.config.GenerationNamespace;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record GeneratedSourceManifest(
        String schemaVersion,
        String runId,
        String profileId,
        String baseUrlHash,
        String namespaceId,
        String pagesPackage,
        String testsPackage,
        List<GeneratedSourceManifestEntry> files
) {

    public static final String SCHEMA_VERSION = "generated-source-manifest.v1";

    public GeneratedSourceManifest {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        runId = requireText(runId, "runId");
        profileId = requireText(profileId, "profileId");
        baseUrlHash = requireText(baseUrlHash, "baseUrlHash");
        namespaceId = requireText(namespaceId, "namespaceId");
        pagesPackage = requireText(pagesPackage, "pagesPackage");
        testsPackage = requireText(testsPackage, "testsPackage");
        files = files == null ? List.of() : files.stream()
                .sorted(Comparator.comparing(GeneratedSourceManifestEntry::relativePath))
                .toList();
        if (files.isEmpty()) {
            throw new IllegalArgumentException("files cannot be empty");
        }
        Set<String> paths = files.stream()
                .map(GeneratedSourceManifestEntry::relativePath)
                .collect(Collectors.toSet());
        if (paths.size() != files.size()) {
            throw new IllegalArgumentException("manifest contains duplicate source paths");
        }
    }

    public static GeneratedSourceManifest create(
            ProjectProfile profile,
            WorkflowRunEnvelope run,
            GeneratedUiSources sources
    ) {
        if (profile == null || run == null || sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("profile, run and generated sources are required");
        }
        GenerationNamespace namespace = GenerationNamespace.resolve(
                profile.profileId(),
                profile.baseUrl(),
                profile.outputProfile().generatedPagesPackage(),
                profile.outputProfile().generatedTestsPackage()
        );
        List<GeneratedSourceManifestEntry> entries = new ArrayList<>();
        sources.pageObjectFiles().forEach(file -> entries.add(entry(GeneratedSourceKind.PAGE_OBJECT, file)));
        sources.uiTestFiles().forEach(file -> entries.add(entry(GeneratedSourceKind.UI_TEST, file)));
        validatePackageOwnership(namespace, entries);
        return new GeneratedSourceManifest(
                SCHEMA_VERSION,
                run.runMetadata().runId(),
                profile.profileId(),
                namespace.baseUrlHash(),
                namespace.namespaceId(),
                namespace.pagesPackage(),
                namespace.testsPackage(),
                entries
        );
    }

    public List<String> persistedPaths() {
        return files.stream().map(GeneratedSourceManifestEntry::relativePath).toList();
    }

    public boolean owns(GeneratedSourceFile source) {
        if (source == null) {
            return false;
        }
        return files.stream().anyMatch(entry -> entry.relativePath().equals(source.relativePath())
                && entry.packageName().equals(source.packageName())
                && entry.className().equals(source.className())
                && entry.contentHash().equals(sha256(source.content())));
    }

    public GeneratedUiSources selectOwned(GeneratedUiSources sources) {
        GeneratedUiSources available = sources == null ? new GeneratedUiSources(null, null) : sources;
        return new GeneratedUiSources(
                available.pageObjectFiles().stream().filter(this::owns).toList(),
                available.uiTestFiles().stream().filter(this::owns).toList()
        );
    }

    private static GeneratedSourceManifestEntry entry(GeneratedSourceKind kind, GeneratedSourceFile file) {
        return new GeneratedSourceManifestEntry(
                kind,
                file.packageName(),
                file.className(),
                file.relativePath(),
                sha256(file.content())
        );
    }

    private static void validatePackageOwnership(
            GenerationNamespace namespace,
            List<GeneratedSourceManifestEntry> entries
    ) {
        List<String> invalid = entries.stream()
                .filter(entry -> entry.kind() == GeneratedSourceKind.PAGE_OBJECT
                        ? !entry.packageName().equals(namespace.pagesPackage())
                        : !entry.packageName().equals(namespace.testsPackage()))
                .map(GeneratedSourceManifestEntry::relativePath)
                .toList();
        if (!invalid.isEmpty()) {
            throw new IllegalStateException("Generated sources escape current project namespace: " + invalid);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte current : digest) {
                result.append(String.format("%02x", current));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash generated source", exception);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
