package ua.demo.agentlab.ai.runtime.skill;

public record RuntimeSkillPrompt(
        String skillId,
        String version,
        String prompt,
        String rules,
        String inputSchema,
        String outputSchema
) {
    public RuntimeSkillPrompt {
        skillId = clean(skillId, "unknown-skill");
        version = clean(version, "unknown");
        prompt = prompt == null ? "" : prompt.trim();
        rules = rules == null ? "" : rules.trim();
        inputSchema = inputSchema == null ? "" : inputSchema.trim();
        outputSchema = outputSchema == null ? "" : outputSchema.trim();
    }

    public String asPromptBlock() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Runtime Skill").append(System.lineSeparator());
        builder.append("skillId=").append(skillId)
                .append(" | version=").append(version)
                .append(System.lineSeparator());
        if (!prompt.isBlank()) {
            builder.append(System.lineSeparator()).append(prompt).append(System.lineSeparator());
        }
        if (!rules.isBlank()) {
            builder.append(System.lineSeparator()).append("# Skill Rules").append(System.lineSeparator());
            builder.append(rules).append(System.lineSeparator());
        }
        if (!outputSchema.isBlank()) {
            builder.append(System.lineSeparator()).append("# Output Schema").append(System.lineSeparator());
            builder.append(outputSchema).append(System.lineSeparator());
        }
        return builder.toString().strip();
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
