package ua.demo.agentlab.ui.discovery.spa;

public record SpaInventoryConfig(
        boolean inventoryEnabled,
        SpaDiscoveryMode mode,
        int maxComponents,
        boolean targetedVerificationEnabled,
        double minConfirmedScore,
        double minLiveVerificationScore,
        int promoteAfterSuccesses,
        int demoteAfterFailures,
        boolean liveVerificationEnabled,
        boolean executeSessionEndingActions,
        boolean executeSafeActions,
        boolean executeDataActions,
        boolean retentionEnabled,
        int degradedRetentionDays,
        int orphanRetentionDays,
        boolean retentionHardDelete
) {
    /** Compatibility constructor for existing callers; live verification and retention use safe defaults. */
    public SpaInventoryConfig(
            boolean inventoryEnabled,
            SpaDiscoveryMode mode,
            int maxComponents,
            boolean targetedVerificationEnabled,
            double minConfirmedScore,
            int promoteAfterSuccesses,
            int demoteAfterFailures
    ) {
        this(inventoryEnabled, mode, maxComponents, targetedVerificationEnabled, minConfirmedScore, 0.65d,
                promoteAfterSuccesses, demoteAfterFailures, true, false, true, false, true, 14, 30, false);
    }

    /** Compatibility constructor for the original live-verification/retention configuration. */
    public SpaInventoryConfig(boolean inventoryEnabled, SpaDiscoveryMode mode, int maxComponents,
                              boolean targetedVerificationEnabled, double minConfirmedScore,
                              int promoteAfterSuccesses, int demoteAfterFailures,
                              boolean liveVerificationEnabled, boolean executeSessionEndingActions,
                              boolean retentionEnabled, int degradedRetentionDays,
                              int orphanRetentionDays, boolean retentionHardDelete) {
        this(inventoryEnabled, mode, maxComponents, targetedVerificationEnabled, minConfirmedScore, 0.65d,
                promoteAfterSuccesses, demoteAfterFailures, liveVerificationEnabled,
                executeSessionEndingActions, true, false, retentionEnabled,
                degradedRetentionDays, orphanRetentionDays, retentionHardDelete);
    }

    public SpaInventoryConfig {
        mode = mode == null ? SpaDiscoveryMode.INVENTORY : mode;
        maxComponents = Math.max(1, maxComponents);
        minConfirmedScore = clamp(minConfirmedScore);
        minLiveVerificationScore = Math.min(minConfirmedScore, clamp(minLiveVerificationScore));
        promoteAfterSuccesses = Math.max(1, promoteAfterSuccesses);
        demoteAfterFailures = Math.max(1, demoteAfterFailures);
        degradedRetentionDays = Math.max(1, degradedRetentionDays);
        orphanRetentionDays = Math.max(degradedRetentionDays, orphanRetentionDays);
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.80d;
    }
}
