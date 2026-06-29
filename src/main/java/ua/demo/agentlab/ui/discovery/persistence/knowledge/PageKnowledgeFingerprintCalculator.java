package ua.demo.agentlab.ui.discovery.persistence.knowledge;

import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
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
                safe(page.title()),
                page.elements().stream()
                        .sorted(Comparator.comparing(MappedElement::elementId))
                        .map(this::elementFingerprint)
                        .collect(Collectors.joining("|")),
                page.forms().stream()
                        .sorted(Comparator.comparing(MappedForm::formId))
                        .map(this::formFingerprint)
                        .collect(Collectors.joining("|")),
                page.actions().stream()
                        .sorted(Comparator.comparing(MappedAction::actionId))
                        .map(this::actionFingerprint)
                        .collect(Collectors.joining("|"))
        );
        return sha256(payload);
    }

    private String elementFingerprint(MappedElement element) {
        return String.join(":",
                safe(element.elementId()),
                safe(element.semanticName()),
                safe(element.elementType()),
                safe(element.role()),
                safe(element.text()),
                String.valueOf(element.clickable()),
                String.valueOf(element.visible()),
                element.supportedActions().stream().sorted().collect(Collectors.joining(",")),
                element.locatorCandidates().stream()
                        .sorted(Comparator.comparing(LocatorCandidate::value))
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
                String.valueOf(locator.sameOrigin()),
                String.valueOf(locator.uniqueOnPage()),
                String.valueOf(locator.stableAcrossRuns())
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

    private String actionFingerprint(MappedAction action) {
        return String.join(":",
                safe(action.actionId()),
                safe(action.actionName()),
                safe(action.actionType()),
                safe(action.sourceElementId()),
                safe(action.targetPageId()),
                safe(action.description())
        );
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
