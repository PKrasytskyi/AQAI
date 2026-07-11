package ua.demo.agentlab.ui.discovery.persistence.knowledge;

import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PageKnowledgeFingerprintCalculator {

    public String fingerprint(MappedPage page) {
        if (page == null) {
            return "";
        }
        String payload = String.join("\n",
                safe(page.pageId()),
                safe(page.pageName()),
                safe(page.pageType()),
                safe(page.urlPattern()),
                page.elements().stream()
                        .filter(this::hasStableFingerprintEvidence)
                        .sorted(Comparator.comparing(this::elementSortKey))
                        .map(this::elementFingerprint)
                        .collect(Collectors.joining("|")),
                page.forms().stream()
                        .sorted(Comparator.comparing(MappedForm::formId))
                        .map(this::formFingerprint)
                        .collect(Collectors.joining("|"))
        );
        return sha256(payload);
    }

    private String elementFingerprint(MappedElement element) {
        return String.join(":",
                safe(element.elementType()),
                safe(element.role()),
                element.locatorCandidates().stream()
                        .filter(this::stableLocatorEvidence)
                        .sorted(Comparator.comparing(locator -> locator.strategy().wireName() + "=" + locator.value()))
                        .map(this::locatorFingerprint)
                        .collect(Collectors.joining(","))
        );
    }

    private String locatorFingerprint(LocatorCandidate locator) {
        return String.join("=",
                locator.strategy().wireName(),
                safe(locator.value()),
                safe(locator.href()),
                safe(locator.originHost()),
                String.valueOf(locator.sameOrigin())
        );
    }

    private String formFingerprint(MappedForm form) {
        return String.join(":",
                safe(form.formId()),
                safe(form.formName()),
                safe(form.action()),
                form.fields().stream()
                        .sorted(Comparator.comparing(MappedField::fieldId))
                        .map(field -> String.join("=",
                                safe(field.fieldId()),
                                safe(field.fieldName()),
                                safe(field.fieldType()),
                                safe(field.label())))
                        .collect(Collectors.joining(",")),
                form.submitActionIds().stream().sorted().collect(Collectors.joining(","))
        );
    }

    private boolean hasStableFingerprintEvidence(MappedElement element) {
        return element != null
                && element.locatorCandidates().stream().anyMatch(this::stableLocatorEvidence);
    }

    private boolean stableLocatorEvidence(LocatorCandidate locator) {
        return locator != null
                && !locator.strategy().isBlank()
                && !safe(locator.value()).isBlank()
                && locator.sameOrigin()
                && locator.stabilityScore() >= 0.75d
                && !transientMenuLocator(locator);
    }

    private boolean transientMenuLocator(LocatorCandidate locator) {
        String value = safe(locator.value()).toLowerCase(java.util.Locale.ROOT);
        String href = safe(locator.href()).toLowerCase(java.util.Locale.ROOT);
        return value.contains("oxd-userdropdown-link")
                || value.contains("/auth/logout")
                || value.contains("/help/support")
                || value.contains("/pim/updatepassword")
                || href.contains("/auth/logout")
                || href.contains("/help/support")
                || href.contains("/pim/updatepassword")
                || value.equals("a[href='#']")
                || value.equals("a[href=\"#\"]");
    }

    private String elementSortKey(MappedElement element) {
        return element.locatorCandidates().stream()
                .filter(this::stableLocatorEvidence)
                .map(locator -> locator.strategy().wireName() + "=" + locator.value())
                .sorted()
                .findFirst()
                .orElse(safe(element.semanticName()) + ":" + safe(element.elementType()));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(safe(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            return Integer.toHexString(safe(value).hashCode());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
