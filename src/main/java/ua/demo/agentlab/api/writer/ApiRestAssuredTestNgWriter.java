package ua.demo.agentlab.api.writer;

import ua.demo.agentlab.api.model.ApiAuthMode;
import ua.demo.agentlab.api.model.ApiAssertionContract;
import ua.demo.agentlab.api.model.HttpMethod;
import ua.demo.agentlab.api.spec.ApiClientMethodSpec;
import ua.demo.agentlab.api.spec.ApiClientSpec;
import ua.demo.agentlab.api.spec.ApiDtoFieldSpec;
import ua.demo.agentlab.api.spec.ApiDtoSpec;
import ua.demo.agentlab.api.spec.ApiGenerationSpec;
import ua.demo.agentlab.api.spec.ApiTestSpec;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ApiRestAssuredTestNgWriter {

    public List<GeneratedSourceFile> write(ApiGenerationSpec spec) {
        if (spec == null) {
            return List.of();
        }
        List<GeneratedSourceFile> files = new ArrayList<>();
        spec.dtoSpecs().stream()
                .sorted(Comparator.comparing(ApiDtoSpec::packageName).thenComparing(ApiDtoSpec::className))
                .map(this::writeDto)
                .forEach(files::add);
        spec.clientSpecs().stream()
                .sorted(Comparator.comparing(ApiClientSpec::className))
                .map(client -> writeClient(client, spec.dtoSpecs()))
                .forEach(files::add);
        spec.testSpecs().stream()
                .sorted(Comparator.comparing(ApiTestSpec::className))
                .map(test -> writeTest(test, spec.clientSpecs(), spec.dtoSpecs()))
                .forEach(files::add);
        return files;
    }

    private GeneratedSourceFile writeDto(ApiDtoSpec spec) {
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(spec.packageName()).append(";").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("public class ").append(spec.className()).append(" {").append(System.lineSeparator()).append(System.lineSeparator());
        for (ApiDtoFieldSpec field : spec.fields()) {
            builder.append("    private ").append(field.javaType()).append(" ").append(field.name()).append(";").append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());
        builder.append("    public ").append(spec.className()).append("() {").append(System.lineSeparator());
        builder.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
        for (ApiDtoFieldSpec field : spec.fields()) {
            String capitalized = capitalize(field.name());
            builder.append("    public ").append(field.javaType()).append(" get").append(capitalized).append("() {").append(System.lineSeparator());
            builder.append("        return ").append(field.name()).append(";").append(System.lineSeparator());
            builder.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
            builder.append("    public void set").append(capitalized).append("(")
                    .append(field.javaType()).append(" ").append(field.name()).append(") {").append(System.lineSeparator());
            builder.append("        this.").append(field.name()).append(" = ").append(field.name()).append(";").append(System.lineSeparator());
            builder.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
        }
        builder.append("}").append(System.lineSeparator());
        return sourceFile("src/main/java", spec.packageName(), spec.className(), builder.toString());
    }

    private GeneratedSourceFile writeClient(ApiClientSpec spec, List<ApiDtoSpec> dtoSpecs) {
        Set<String> imports = new LinkedHashSet<>();
        imports.add("ua.demo.agentlab.core.api.ApiManager");
        imports.add("io.restassured.response.Response");
        for (ApiClientMethodSpec method : spec.methods()) {
            if (method.hasRequestBody()) {
                dtoSpecs.stream()
                        .filter(dto -> dto.className().equals(method.requestDtoClassName()))
                        .findFirst()
                        .ifPresent(dto -> imports.add(dto.packageName() + "." + dto.className()));
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(spec.packageName()).append(";").append(System.lineSeparator()).append(System.lineSeparator());
        imports.forEach(importName -> builder.append("import ").append(importName).append(";").append(System.lineSeparator()));
        builder.append(System.lineSeparator());
        builder.append("public class ").append(spec.className()).append(" {").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("    private static final String ").append(spec.endpointConstantName())
                .append(" = \"").append(spec.endpointPath()).append("\";").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("    private final ApiManager apiManager;").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("    public ").append(spec.className()).append("(ApiManager apiManager) {").append(System.lineSeparator());
        builder.append("        this.apiManager = apiManager;").append(System.lineSeparator());
        builder.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
        for (ApiClientMethodSpec method : spec.methods()) {
            appendClientMethod(builder, spec, method);
        }
        builder.append("}").append(System.lineSeparator());
        return sourceFile("src/main/java", spec.packageName(), spec.className(), builder.toString());
    }

    private void appendClientMethod(StringBuilder builder, ApiClientSpec client, ApiClientMethodSpec method) {
        builder.append("    public Response ").append(method.methodName()).append("(")
                .append(parameters(method)).append(") {").append(System.lineSeparator());
        builder.append("        return ").append(requestFactory(method.authMode())).append(System.lineSeparator());
        if (method.authMode() == ApiAuthMode.UNAUTHORIZED) {
            builder.append("                .header(\"Authorization\", \"\")").append(System.lineSeparator());
        }
        for (String pathParameter : method.pathParameters()) {
            builder.append("                .pathParam(\"").append(pathParameter).append("\", ").append(pathParameter).append(")").append(System.lineSeparator());
        }
        if (method.hasRequestBody()) {
            builder.append("                .body(bodyRequest)").append(System.lineSeparator());
        }
        builder.append("                .when()").append(System.lineSeparator());
        builder.append("                .").append(restAssuredVerb(method.httpMethod())).append("(")
                .append(pathExpression(client, method)).append(");").append(System.lineSeparator());
        builder.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
    }

    private GeneratedSourceFile writeTest(ApiTestSpec spec, List<ApiClientSpec> clients, List<ApiDtoSpec> dtoSpecs) {
        ApiClientSpec client = clients.stream()
                .filter(candidate -> candidate.className().equals(spec.clientClassName()))
                .findFirst()
                .orElse(null);
        ApiClientMethodSpec method = client == null ? null : client.methods().stream()
                .filter(candidate -> candidate.methodName().equals(spec.clientMethodName()))
                .findFirst()
                .orElse(null);
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(spec.packageName()).append(";").append(System.lineSeparator()).append(System.lineSeparator());
        if (client != null) {
            builder.append("import ").append(client.packageName()).append(".").append(client.className()).append(";").append(System.lineSeparator());
        }
        if (method != null && method.hasRequestBody()) {
            dtoSpecs.stream()
                    .filter(dto -> dto.className().equals(method.requestDtoClassName()))
                    .findFirst()
                    .ifPresent(dto -> builder.append("import ")
                            .append(dto.packageName())
                            .append(".")
                            .append(dto.className())
                            .append(";")
                            .append(System.lineSeparator()));
        }
        builder.append("import ua.demo.agentlab.core.api.ApiManager;").append(System.lineSeparator());
        builder.append("import io.restassured.response.Response;").append(System.lineSeparator());
        builder.append("import org.testng.annotations.Test;").append(System.lineSeparator());
        builder.append("import ua.demo.agentlab.core.api.assertions.ApiAssertions;").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("public class ").append(spec.className()).append(" {").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("    private final ApiManager apiManager = new ApiManager();").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("    @Test").append(System.lineSeparator());
        builder.append("    public void ").append(spec.testMethodName()).append("() {").append(System.lineSeparator());
        builder.append("        ").append(spec.clientClassName()).append(" client = new ").append(spec.clientClassName()).append("(apiManager);").append(System.lineSeparator());
        builder.append("        Response response = client.").append(spec.clientMethodName()).append("(")
                .append(testMethodArguments(method)).append(");").append(System.lineSeparator());
        appendAssertions(builder, spec.assertionContract());
        builder.append("    }").append(System.lineSeparator());
        builder.append("}").append(System.lineSeparator());
        return sourceFile("src/test/java", spec.packageName(), spec.className(), builder.toString());
    }

    private void appendAssertions(StringBuilder builder, ApiAssertionContract contract) {
        if (contract == null) {
            builder.append("        ApiAssertions.assertStatusCode(response.statusCode(), 200);").append(System.lineSeparator());
            return;
        }
        builder.append("        ApiAssertions.assertStatusCode(response.statusCode(), ")
                .append(contract.expectedStatusCode()).append(");").append(System.lineSeparator());
        contract.bodyAssertions().forEach(assertion -> {
            if (!assertion.jsonPath().isBlank()) {
                builder.append("        ApiAssertions.assertFieldExists(response.jsonPath().get(\"")
                        .append(assertion.jsonPath()).append("\"), \"")
                        .append(assertion.jsonPath()).append("\");").append(System.lineSeparator());
            }
        });
    }

    private String parameters(ApiClientMethodSpec method) {
        List<String> parameters = new ArrayList<>();
        for (String pathParameter : method.pathParameters()) {
            parameters.add(("id".equalsIgnoreCase(pathParameter) ? "int " : "String ") + pathParameter);
        }
        if (method.hasRequestBody()) {
            parameters.add(method.requestDtoClassName() + " bodyRequest");
        }
        return String.join(", ", parameters);
    }

    private String testMethodArguments(ApiClientMethodSpec method) {
        if (method == null) {
            return "";
        }
        List<String> arguments = new ArrayList<>();
        for (String pathParameter : method.pathParameters()) {
            arguments.add("1");
        }
        if (method.hasRequestBody()) {
            arguments.add("new " + method.requestDtoClassName() + "()");
        }
        return String.join(", ", arguments);
    }

    private String requestFactory(ApiAuthMode authMode) {
        return switch (authMode) {
            case AUTHORIZED -> "apiManager.newAuthorizedRequest()";
            case UNAUTHORIZED -> "apiManager.newUnAuthorizedRequest()";
            case NONE -> "apiManager.newRequest()";
        };
    }

    private String pathExpression(ApiClientSpec client, ApiClientMethodSpec method) {
        String suffix = suffix(client.endpointPath(), method.pathTemplate());
        if (suffix.isBlank()) {
            return client.endpointConstantName();
        }
        return client.endpointConstantName() + " + \"" + suffix + "\"";
    }

    private String suffix(String endpointPath, String pathTemplate) {
        if (pathTemplate.startsWith(endpointPath)) {
            return pathTemplate.substring(endpointPath.length());
        }
        return pathTemplate;
    }

    private String restAssuredVerb(HttpMethod method) {
        return method.name().toLowerCase(java.util.Locale.ROOT);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private GeneratedSourceFile sourceFile(String sourceRoot, String packageName, String className, String content) {
        return new GeneratedSourceFile(
                packageName,
                className,
                sourceRoot + "/" + packageName.replace('.', '/') + "/" + className + ".java",
                content
        );
    }
}
