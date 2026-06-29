package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelBuilder;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFlowModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.List;

public class RuleBasedPageMapper implements PageMapper {

    private final PageModelMappedPageMapper pageMapper;
    private final PageKnowledgeArtifactBuilder artifactBuilder;

    public RuleBasedPageMapper() {
        this(new PageModelMappedPageMapper(), new PageKnowledgeArtifactBuilder());
    }

    public RuleBasedPageMapper(
            PageModelMappedPageMapper pageMapper,
            PageKnowledgeArtifactBuilder artifactBuilder
    ) {
        this.pageMapper = pageMapper == null ? new PageModelMappedPageMapper() : pageMapper;
        this.artifactBuilder = artifactBuilder == null ? new PageKnowledgeArtifactBuilder() : artifactBuilder;
    }

    @Override
    public MappedUiKnowledge map(
            UiDiscoverySnapshot snapshot,
            SeleniumDiscoveryResult seleniumDiscoveryResult,
            PageModelBundle pageModelBundle
    ) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }

        PageModelBundle sourceBundle = pageModelBundle == null || pageModelBundle.pages().isEmpty()
                ? new PageModelBuilder().build(snapshot, seleniumDiscoveryResult)
                : pageModelBundle;
        List<MappedPage> pages = sourceBundle.pages().stream()
                .map(pageMapper::map)
                .toList();
        List<MappedTransition> transitions = sourceBundle.pages().stream()
                .flatMap(page -> page.flows().stream())
                .map(this::mapTransition)
                .toList();

        return new MappedUiKnowledge(
                pages,
                transitions,
                artifactBuilder.buildGraphNodes(pages),
                artifactBuilder.buildGraphEdges(pages, transitions),
                artifactBuilder.buildVectorDocuments(pages, transitions)
        );
    }

    private MappedTransition mapTransition(PageFlowModel flow) {
        return new MappedTransition(
                flow.flowId(),
                flow.fromPageId(),
                flow.fromPageId() + ":action:" + sanitize(flow.actionLabel() + "-" + flow.actionType()),
                flow.toPageId(),
                flow.toUrl(),
                normalizeActionType(flow.actionType()),
                flow.success(),
                flow.success() ? 0.95d : 0.40d
        );
    }

    private String normalizeActionType(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return normalized.isBlank() ? "navigate" : normalized;
    }

    private String sanitize(String value) {
        String normalized = value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "action" : normalized;
    }
}
