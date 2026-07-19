package ua.demo.agentlab.ui.discovery.interaction.promotion;

public record PromotionHistory(int successes, int failures) {
    public PromotionHistory {
        successes = Math.max(0, successes);
        failures = Math.max(0, failures);
    }

    public static PromotionHistory empty() { return new PromotionHistory(0, 0); }
    public double passRate() { return successes + failures == 0 ? 0.0d : (double) successes / (successes + failures); }
    public double flakyRate() { return successes + failures == 0 ? 0.0d : (double) failures / (successes + failures); }
}
