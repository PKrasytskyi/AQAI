package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.mapping.RuleBasedPageMapper;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelBuilder;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTargetPageSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts rendered live transition targets into current-run SPA inventory and merges them by route. */
public final class LiveTargetInventoryAssembler {
    private final PageModelBuilder pageModelBuilder;
    private final RuleBasedPageMapper pageMapper;
    private final SpaInventoryBuilder inventoryBuilder;
    private final SpaPageInventoryMergeService pageMergeService;

    public LiveTargetInventoryAssembler() {
        this(new PageModelBuilder(), new RuleBasedPageMapper(), new SpaInventoryBuilder(),
                new SpaPageInventoryMergeService());
    }

    LiveTargetInventoryAssembler(PageModelBuilder pageModelBuilder, RuleBasedPageMapper pageMapper,
                                 SpaInventoryBuilder inventoryBuilder) {
        this(pageModelBuilder, pageMapper, inventoryBuilder, new SpaPageInventoryMergeService());
    }

    LiveTargetInventoryAssembler(PageModelBuilder pageModelBuilder, RuleBasedPageMapper pageMapper,
                                 SpaInventoryBuilder inventoryBuilder,
                                 SpaPageInventoryMergeService pageMergeService) {
        this.pageModelBuilder = pageModelBuilder;
        this.pageMapper = pageMapper;
        this.inventoryBuilder = inventoryBuilder;
        this.pageMergeService = pageMergeService;
    }

    public SpaInventoryBundle merge(ProjectProfile profile, SpaInventoryBundle baseInventory,
                                    List<LiveTargetPageSnapshot> liveTargets,
                                    KnowledgeRunMetadata metadata, SpaInventoryConfig config) {
        if (baseInventory == null) {
            throw new IllegalArgumentException("baseInventory cannot be null");
        }
        if (profile == null || liveTargets == null || liveTargets.isEmpty()) {
            return baseInventory;
        }
        var snapshots = liveTargets.stream().map(LiveTargetPageSnapshot::snapshot).toList();
        UiDiscoverySnapshot discovery = new UiDiscoverySnapshot(profile.profileId(), profile.projectName(),
                "live-target-state", List.of(), List.of());
        SeleniumDiscoveryResult selenium = new SeleniumDiscoveryResult(profile.baseUrl(), snapshots, List.of());
        var pageModels = pageModelBuilder.build(discovery, selenium);
        var mapped = pageMapper.map(discovery, selenium, pageModels);
        SpaInventoryBundle targets = inventoryBuilder.build(pageModels, mapped, metadata,
                inventoryConfig(config), null, List.of());

        Map<String, SpaPageInventory> byRoute = new LinkedHashMap<>();
        baseInventory.pages().forEach(page -> byRoute.put(routeKey(page.route()), page));
        targets.pages().forEach(page -> byRoute.merge(routeKey(page.route()), page, pageMergeService::merge));
        List<String> trace = new ArrayList<>(baseInventory.sourceTrace());
        trace.add("spa-inventory:live-target-merge");
        trace.add("live-target-pages=" + targets.pages().size());
        return new SpaInventoryBundle(baseInventory.schemaVersion(), baseInventory.mode(),
                List.copyOf(byRoute.values()), trace);
    }

    private SpaInventoryConfig inventoryConfig(SpaInventoryConfig source) {
        SpaInventoryConfig config = source == null
                ? new SpaInventoryConfig(true, SpaDiscoveryMode.INVENTORY, 30, true, 0.80d, 2, 2)
                : source;
        return new SpaInventoryConfig(true, SpaDiscoveryMode.INVENTORY, config.maxComponents(),
                config.targetedVerificationEnabled(), config.minConfirmedScore(), config.minLiveVerificationScore(),
                config.promoteAfterSuccesses(), config.demoteAfterFailures(), config.liveVerificationEnabled(),
                config.executeSessionEndingActions(), config.executeSafeActions(), config.executeDataActions(),
                config.retentionEnabled(), config.degradedRetentionDays(), config.orphanRetentionDays(),
                config.retentionHardDelete());
    }

    private String routeKey(String route) {
        return ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.canonicalize(route).toLowerCase();
    }
}
