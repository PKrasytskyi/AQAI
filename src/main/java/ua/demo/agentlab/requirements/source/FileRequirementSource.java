package ua.demo.agentlab.requirements.source;

import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileRequirementSource implements RequirementSource {

    @Override
    public boolean supports(RequirementInput input) {
        return input.sourceType() == SourceType.FILE;
    }

    @Override
    public RequirementDocument load(RequirementInput input) {
        try {
            String content = Files.readString(Path.of(input.location()));
            return new RequirementDocument(input.location(), normalize(content));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read file: " + input.location(), exception);
        }
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().replace("\r\n", "\n");
    }
}
