package ua.demo.agentlab.ai.runtime.skill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class RuntimeSkillPromptLoader {

    private static final String DEFAULT_ROOT = "runtime-skills";

    public RuntimeSkillPrompt load(String skillId) {
        String normalizedSkillId = clean(skillId);
        Path skillRoot = root().resolve(normalizedSkillId);
        return new RuntimeSkillPrompt(
                normalizedSkillId,
                version(read(skillRoot.resolve("skill.yaml"))),
                read(skillRoot.resolve("prompt.md")),
                read(skillRoot.resolve("rules.md")),
                read(skillRoot.resolve("input-schema.json")),
                read(skillRoot.resolve("output-schema.json"))
        );
    }

    public String promptBlock(String skillId) {
        return load(skillId).asPromptBlock();
    }

    private Path root() {
        String configured = firstNonBlank(
                System.getProperty("runtime.skills.root"),
                System.getenv("RUNTIME_SKILLS_ROOT")
        );
        return Path.of(configured == null ? DEFAULT_ROOT : configured);
    }

    private String read(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private String version(String skillYaml) {
        if (skillYaml == null || skillYaml.isBlank()) {
            return "unknown";
        }
        return skillYaml.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("version:"))
                .map(line -> line.substring("version:".length()).trim())
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("unknown");
    }

    private String clean(String skillId) {
        String value = skillId == null ? "" : skillId.trim();
        if (!value.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid runtime skill id: " + skillId);
        }
        return value;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }
}

