package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTargetPageSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Debug boundary for target DOM capture, effective inventory, and second-pass source binding. */
public final class LiveTargetMappingArtifactWriter {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public List<String> write(List<LiveTargetPageSnapshot> snapshots, UiInteractionInventory inventory,
                              SourceStateBindingBundle bindings) {
        try {
            Path directory = Path.of("target", "discovery");
            Files.createDirectories(directory);
            List<String> artifacts = new ArrayList<>();
            artifacts.add(write(directory.resolve("spa-live-target-pages.json"), snapshots == null ? List.of() : snapshots));
            artifacts.add(write(directory.resolve("spa-effective-inventory.json"), inventory));
            artifacts.add(write(directory.resolve("spa-rebound-source-state-bindings.json"), bindings));
            return List.copyOf(artifacts);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write live target mapping artifacts", exception);
        }
    }

    private String write(Path path, Object value) throws Exception {
        mapper.writeValue(path.toFile(), value);
        return path.toAbsolutePath().toString();
    }
}
