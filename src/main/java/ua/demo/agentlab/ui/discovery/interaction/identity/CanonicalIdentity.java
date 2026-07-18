package ua.demo.agentlab.ui.discovery.interaction.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class CanonicalIdentity {

    private CanonicalIdentity() { }

    static String text(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    static String occurrenceText(String value) {
        return text(value).replaceAll("-\\d+$", "");
    }

    static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < 8; index++) result.append(String.format("%02x", bytes[index]));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot build canonical UI identity", exception);
        }
    }
}
