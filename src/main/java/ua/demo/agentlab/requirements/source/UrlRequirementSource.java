package ua.demo.agentlab.requirements.source;

import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class UrlRequirementSource implements RequirementSource {

    @Override
    public boolean supports(RequirementInput input) {
        return input.sourceType() == SourceType.URL;
    }

    @Override
    public RequirementDocument load(RequirementInput input) {
        try (var stream = URI.create(input.location()).toURL().openStream()) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new RequirementDocument(input.location(), normalize(content));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read URL: " + input.location(), exception);
        }
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().replace("\r\n", "\n");
    }
}
