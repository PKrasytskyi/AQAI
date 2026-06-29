package ua.demo.agentlab.ui.flow;

import ua.demo.agentlab.ui.discovery.model.DiscoveredUiFlow;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.flow.model.CanonicalFlow;
import ua.demo.agentlab.ui.flow.model.CanonicalFlowStep;
import ua.demo.agentlab.ui.flow.model.CanonicalPage;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

import java.util.ArrayList;
import java.util.List;

public class RuleBasedCanonicalPageFlowMapper implements CanonicalPageFlowMapper {

    @Override
    public CanonicalPageFlowModel map(UiDiscoverySnapshot discoverySnapshot) {
        if (discoverySnapshot == null) {
            throw new IllegalArgumentException("discoverySnapshot cannot be null");
        }

        List<CanonicalPage> pages = discoverySnapshot.pages().stream()
                .map(this::toCanonicalPage)
                .toList();

        List<CanonicalFlow> flows = discoverySnapshot.flows().stream()
                .map(this::toCanonicalFlow)
                .toList();

        return new CanonicalPageFlowModel(
                discoverySnapshot.projectProfileId(),
                discoverySnapshot.projectName(),
                pages,
                flows
        );
    }

    private CanonicalPage toCanonicalPage(DiscoveredUiPage page) {
        return new CanonicalPage(
                page.pageName(),
                page.route(),
                page.capabilities(),
                page.locatorHints(),
                page.discoveryReason(),
                page.canonicalPageType(),
                page.pageIdentity()
        );
    }

    private CanonicalFlow toCanonicalFlow(DiscoveredUiFlow flow) {
        List<CanonicalFlowStep> steps = new ArrayList<>();
        for (int index = 0; index < flow.stepDescriptions().size(); index++) {
            steps.add(new CanonicalFlowStep(index + 1, flow.stepDescriptions().get(index), flow.targetPageName()));
        }

        return new CanonicalFlow(
                flow.flowId(),
                flow.flowName(),
                flow.flowType(),
                flow.sourcePageName(),
                flow.sourceRoute(),
                flow.targetPageName(),
                flow.targetRoute(),
                flow.authenticationRequired(),
                steps,
                flow.expectedOutcomes(),
                flow.matchKeywords(),
                flow.sourceRequirementIds()
        );
    }
}
