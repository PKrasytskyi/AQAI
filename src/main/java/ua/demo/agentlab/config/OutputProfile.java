package ua.demo.agentlab.config;

public record OutputProfile(
        String generatedPagesPackage,
        String generatedTestsPackage
) {
    public OutputProfile {
        generatedPagesPackage = requireText(generatedPagesPackage, "generatedPagesPackage");
        generatedTestsPackage = requireText(generatedTestsPackage, "generatedTestsPackage");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
