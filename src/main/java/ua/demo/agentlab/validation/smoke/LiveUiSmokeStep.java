package ua.demo.agentlab.validation.smoke;

public record LiveUiSmokeStep(
        String name,
        String status,
        String detail
) {
    public LiveUiSmokeStep {
        name = clean(name);
        status = clean(status);
        detail = clean(detail);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
