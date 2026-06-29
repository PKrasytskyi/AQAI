package ua.demo.agentlab.policy.model;

import java.util.Objects;

public record NamingPolicy(
        String pageObjectSuffix,
        String uiTestSuffix,
        String apiTestSuffix,
        String locatorMethodSuffix,
        String positiveMethodPrefix,
        String negativeMethodPrefix,
        boolean keepRequirementsWords,
        CaseStyle classCaseStyle,
        CaseStyle methodCaseStyle
        ) {

    public NamingPolicy {
        pageObjectSuffix = requireText(pageObjectSuffix, "pageObjectSuffix");
        uiTestSuffix = requireText(uiTestSuffix, "uiTestSuffix");
        apiTestSuffix = requireText(apiTestSuffix, "apiTestSuffix");
        locatorMethodSuffix = requireText(locatorMethodSuffix, "locatorMethodSuffix");
        positiveMethodPrefix = requireText(positiveMethodPrefix, "positiveMethodPrefix");
        negativeMethodPrefix = requireText(negativeMethodPrefix, "negativeMethodPrefix");
        Objects.requireNonNull(classCaseStyle, "classCaseStyle");
        Objects.requireNonNull(methodCaseStyle, "methodCaseStyle");
    }

    private static String requireText(String value, String fieldName){
        if(value == null || value.isBlank()){
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    public enum CaseStyle {
        PASCAL_CASE,
        CAMEL_CASE
    }
}
