package ua.demo.agentlab.ai.ui.prompt;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PromptNormalizer {

    public String normalize(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        String lineNormalized = prompt.replace("\r\n", "\n").replace('\r', '\n');
        String withoutTrailingSpaces = Arrays.stream(lineNormalized.split("\n", -1))
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"));
        return withoutTrailingSpaces
                .replaceAll("\n{3,}", "\n\n")
                .strip();
    }
}
