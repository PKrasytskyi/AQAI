package ua.demo.agentlab.ai.token;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

public class OpenAiTokenCounter {

    private final EncodingRegistry registry;
    private final String modelName;

    public OpenAiTokenCounter(String modelName) {
        this(Encodings.newDefaultEncodingRegistry(), modelName);
    }

    OpenAiTokenCounter(EncodingRegistry registry, String modelName) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        this.registry = registry;
        this.modelName = modelName == null || modelName.isBlank() ? "gpt-5-mini" : modelName.trim();
    }

    public TokenCountResult count(String text) {
        String safeText = text == null ? "" : text;
        try {
            Encoding encoding = encoding();
            return new TokenCountResult(
                    encoding.countTokens(safeText),
                    "openai-tokenizer",
                    modelName + "/" + encoding.getName()
            );
        } catch (Exception exception) {
            return new TokenCountResult(
                    (int) Math.ceil(safeText.length() / 4.0d),
                    "char-estimate-fallback",
                    modelName
            );
        }
    }

    private Encoding encoding() {
        return registry.getEncodingForModel(modelName)
                .orElseGet(() -> registry.getEncoding(encodingTypeFor(modelName)));
    }

    private EncodingType encodingTypeFor(String model) {
        String normalized = model == null ? "" : model.toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("gpt-5") || normalized.startsWith("gpt-4o")) {
            return EncodingType.O200K_BASE;
        }
        return EncodingType.CL100K_BASE;
    }
}
