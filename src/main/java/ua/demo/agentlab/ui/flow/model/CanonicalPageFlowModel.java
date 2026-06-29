package ua.demo.agentlab.ui.flow.model;

import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record CanonicalPageFlowModel(
        String projectProfileId,
        String projectName,
        List<CanonicalPage> pages,
        List<CanonicalFlow> flows
) {
    public Optional<CanonicalPage> findPage(String pageName) {
        return pages.stream()
                .filter(page -> PageReferenceMatcher.matches(page, pageName))
                .findFirst();
    }

    public Optional<CanonicalFlow> findBestFlow(String scenarioTitle) {
        Set<String> scenarioTokens = tokenize(scenarioTitle);

        return flows.stream()
                .map(flow -> new FlowScore(flow, score(flow, scenarioTokens)))
                .filter(score -> score.score() > 0)
                .sorted(Comparator.comparingInt(FlowScore::score).reversed())
                .map(FlowScore::flow)
                .findFirst();
    }

    private int score(CanonicalFlow flow, Set<String> scenarioTokens) {
        int score = 0;
        for (String keyword : flow.matchKeywords()) {
            if (scenarioTokens.contains(keyword.toLowerCase(Locale.ROOT))) {
                score++;
            }
        }
        for (String token : tokenize(flow.flowName())) {
            if (scenarioTokens.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        return List.of(text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .stream()
                .filter(token -> token.length() >= 4)
                .collect(Collectors.toSet());
    }

    private record FlowScore(CanonicalFlow flow, int score) {
    }
}
