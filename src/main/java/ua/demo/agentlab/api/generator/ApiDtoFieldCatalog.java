package ua.demo.agentlab.api.generator;

import ua.demo.agentlab.api.spec.ApiDtoFieldSpec;
import ua.demo.agentlab.api.spec.ApiDtoKind;

import java.util.List;
import java.util.Locale;

class ApiDtoFieldCatalog {

    List<ApiDtoFieldSpec> fields(String resourceToken, ApiDtoKind kind) {
        String normalized = resourceToken == null ? "" : resourceToken.toLowerCase(Locale.ROOT);
        if ("users".equals(normalized)) {
            return userFields(kind);
        }
        if ("posts".equals(normalized)) {
            return postFields(kind);
        }
        if ("comments".equals(normalized)) {
            return commentFields(kind);
        }
        if ("todos".equals(normalized)) {
            return todoFields(kind);
        }
        return fallbackFields(kind);
    }

    private List<ApiDtoFieldSpec> userFields(ApiDtoKind kind) {
        if (kind == ApiDtoKind.RESPONSE) {
            return prependId(List.of(
                    field("name", "String", true),
                    field("email", "String", true),
                    field("gender", "String", true),
                    field("status", "String", true)
            ));
        }
        return List.of(
                field("name", "String", true),
                field("email", "String", true),
                field("gender", "String", true),
                field("status", "String", true)
        );
    }

    private List<ApiDtoFieldSpec> postFields(ApiDtoKind kind) {
        List<ApiDtoFieldSpec> fields = List.of(
                field("userId", "int", true),
                field("title", "String", true),
                field("body", "String", true)
        );
        return kind == ApiDtoKind.RESPONSE ? prependId(fields) : fields;
    }

    private List<ApiDtoFieldSpec> commentFields(ApiDtoKind kind) {
        List<ApiDtoFieldSpec> fields = List.of(
                field("postId", "int", true),
                field("name", "String", true),
                field("email", "String", true),
                field("body", "String", true)
        );
        return kind == ApiDtoKind.RESPONSE ? prependId(fields) : fields;
    }

    private List<ApiDtoFieldSpec> todoFields(ApiDtoKind kind) {
        List<ApiDtoFieldSpec> fields = List.of(
                field("userId", "int", true),
                field("title", "String", true),
                field("dueOn", "String", false),
                field("status", "String", true)
        );
        return kind == ApiDtoKind.RESPONSE ? prependId(fields) : fields;
    }

    private List<ApiDtoFieldSpec> fallbackFields(ApiDtoKind kind) {
        List<ApiDtoFieldSpec> fields = List.of(
                field("name", "String", false),
                field("value", "String", false)
        );
        return kind == ApiDtoKind.RESPONSE ? prependId(fields) : fields;
    }

    private List<ApiDtoFieldSpec> prependId(List<ApiDtoFieldSpec> fields) {
        java.util.ArrayList<ApiDtoFieldSpec> output = new java.util.ArrayList<>();
        output.add(field("id", "int", true));
        output.addAll(fields);
        return List.copyOf(output);
    }

    private ApiDtoFieldSpec field(String name, String type, boolean required) {
        return new ApiDtoFieldSpec(name, type, required);
    }
}
