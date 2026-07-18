package ua.demo.agentlab.ui.discovery.component;

import ua.demo.agentlab.ui.discovery.component.model.ComponentDiscoveryModel;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentModel;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentPageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.component.strategy.*;

import java.util.List;

public class ComponentBoundaryDetector {

    private final ScopedLocatorValidationService scopedLocatorValidationService;
    private final List<ComponentDetectionStrategy> strategies;
    private final ComponentBoundaryMergeService mergeService;

    public ComponentBoundaryDetector() {
        this(new ScopedLocatorValidationService());
    }

    public ComponentBoundaryDetector(ScopedLocatorValidationService scopedLocatorValidationService) {
        this.scopedLocatorValidationService = scopedLocatorValidationService == null
                ? new ScopedLocatorValidationService()
                : scopedLocatorValidationService;
        this.strategies = List.of(
                new HeaderAndUserMenuDetector(),
                new ModalAndOverlayDetector(),
                new FormComponentDetector(),
                new TableAndResultsDetector(),
                new SearchComponentDetector(),
                new AriaLandmarkComponentDetector(),
                new NavigationComponentDetector(),
                new GenericContentDetector()
        );
        this.mergeService = new ComponentBoundaryMergeService(this.scopedLocatorValidationService);
    }

    public ComponentDiscoveryModel detect(PageModelBundle bundle) {
        if (bundle == null || bundle.pages().isEmpty()) {
            return ComponentDiscoveryModel.empty("component:no-page-models");
        }
        List<SemanticComponentPageModel> pages = bundle.pages().stream()
                .map(this::detectPage)
                .toList();
        return new ComponentDiscoveryModel(pages, List.of("component:page-model-bundle"));
    }

    private SemanticComponentPageModel detectPage(PageModel page) {
        ComponentDetectionContext context = new ComponentDetectionContext(page);
        List<ComponentCandidate> candidates = strategies.stream().flatMap(strategy -> strategy.detect(context).stream())
                .toList();
        List<SemanticComponentModel> components = mergeService.merge(context, candidates);

        double confidence = components.stream()
                .mapToDouble(SemanticComponentModel::confidence)
                .average()
                .orElse(0.0d);
        return new SemanticComponentPageModel(
                page.pageId(),
                page.route(),
                page.featureGuess(),
                components,
                confidence
        );
    }

}
