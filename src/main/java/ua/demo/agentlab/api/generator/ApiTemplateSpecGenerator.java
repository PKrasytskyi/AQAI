package ua.demo.agentlab.api.generator;

import ua.demo.agentlab.api.model.ApiAssertionContract;
import ua.demo.agentlab.api.model.ApiAuthMode;
import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.ApiEndpointModel;
import ua.demo.agentlab.api.model.CanonicalApiTestCase;
import ua.demo.agentlab.api.model.CanonicalApiTestCaseBundle;
import ua.demo.agentlab.api.model.HttpMethod;
import ua.demo.agentlab.api.spec.ApiClientMethodSpec;
import ua.demo.agentlab.api.spec.ApiClientSpec;
import ua.demo.agentlab.api.spec.ApiDtoKind;
import ua.demo.agentlab.api.spec.ApiDtoSpec;
import ua.demo.agentlab.api.spec.ApiGenerationSpec;
import ua.demo.agentlab.api.spec.ApiTestSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ApiTemplateSpecGenerator {

    private static final String CLIENT_PACKAGE = "ua.demo.agentlab.api.generated.clients";
    private static final String REQUEST_PACKAGE = "ua.demo.agentlab.api.generated.models.request";
    private static final String UPDATE_PACKAGE = "ua.demo.agentlab.api.generated.models.update";
    private static final String RESPONSE_PACKAGE = "ua.demo.agentlab.api.generated.models.response";
    private static final String TEST_PACKAGE = "ua.demo.agentlab.api.generated.tests";

    private final ApiDtoFieldCatalog fieldCatalog = new ApiDtoFieldCatalog();

    public ApiGenerationSpec generate(
            ApiEndpointBundle endpointBundle,
            CanonicalApiTestCaseBundle testCaseBundle
    ) {
        List<ApiEndpointModel> endpoints = endpointBundle == null ? List.of() : endpointBundle.endpoints();
        Map<String, List<ApiEndpointModel>> grouped = groupByEndpointPath(endpoints);
        List<ApiClientSpec> clients = new ArrayList<>();
        List<ApiDtoSpec> dtos = new ArrayList<>();
        Map<String, ApiClientSpec> clientsByEndpointId = new LinkedHashMap<>();

        for (List<ApiEndpointModel> resourceEndpoints : grouped.values()) {
            ApiResourceDescriptor resource = describeResource(resourceEndpoints.get(0));
            List<ApiClientMethodSpec> methods = resourceEndpoints.stream()
                    .sorted(Comparator.comparing(ApiEndpointModel::path).thenComparing(endpoint -> endpoint.method().name()))
                    .flatMap(endpoint -> clientMethods(resource, endpoint).stream())
                    .toList();
            ApiClientSpec client = new ApiClientSpec(
                    CLIENT_PACKAGE,
                    resource.singularName() + "Client",
                    resource.pluralName().toUpperCase(Locale.ROOT) + "_ENDPOINT",
                    resource.endpointPath(),
                    methods
            );
            clients.add(client);
            resourceEndpoints.forEach(endpoint -> clientsByEndpointId.put(endpoint.endpointId(), client));
            dtos.addAll(dtoSpecs(resource, resourceEndpoints));
        }

        return new ApiGenerationSpec(
                clients,
                deduplicateDtos(dtos),
                testSpecs(testCaseBundle, clientsByEndpointId)
        );
    }

    private Map<String, List<ApiEndpointModel>> groupByEndpointPath(List<ApiEndpointModel> endpoints) {
        Map<String, List<ApiEndpointModel>> grouped = new LinkedHashMap<>();
        for (ApiEndpointModel endpoint : endpoints) {
            grouped.computeIfAbsent(baseEndpointPath(endpoint.path()), ignored -> new ArrayList<>()).add(endpoint);
        }
        return grouped;
    }

    private ApiResourceDescriptor describeResource(ApiEndpointModel endpoint) {
        String endpointPath = baseEndpointPath(endpoint.path());
        String resourceToken = resourceToken(endpointPath);
        String singular = toClassName(singularize(resourceToken));
        String plural = toConstantToken(resourceToken);
        return new ApiResourceDescriptor(resourceToken, singular, plural, endpointPath);
    }

    private List<ApiClientMethodSpec> clientMethods(ApiResourceDescriptor resource, ApiEndpointModel endpoint) {
        List<String> pathParameters = pathParameters(endpoint.path());
        ApiResourceDescriptor endpointResource = endpointResourceDescriptor(resource, endpoint);
        String requestDto = requestDtoClass(endpointResource.singularName(), endpoint.method());
        String responseDto = responseDtoClass(endpointResource.singularName());
        ApiAuthMode authMode = authMode(endpoint);
        String methodName = methodName(resource, endpoint, pathParameters);
        ApiClientMethodSpec primary = new ApiClientMethodSpec(
                methodName,
                endpoint.method(),
                endpoint.path(),
                requestDto,
                responseDto,
                authMode,
                false,
                pathParameters
        );
        if (endpoint.method() == HttpMethod.POST) {
            return List.of(primary, new ApiClientMethodSpec(
                    methodName + "WithoutAuthorization",
                    endpoint.method(),
                    endpoint.path(),
                    requestDto,
                    responseDto,
                    ApiAuthMode.UNAUTHORIZED,
                    false,
                    pathParameters
            ));
        }
        return List.of(primary);
    }

    private ApiResourceDescriptor endpointResourceDescriptor(ApiResourceDescriptor clientResource, ApiEndpointModel endpoint) {
        String resourceToken = terminalResourceToken(endpoint.path());
        if (resourceToken.isBlank()) {
            return clientResource;
        }
        String singular = toClassName(singularize(resourceToken));
        String plural = toConstantToken(resourceToken);
        return new ApiResourceDescriptor(resourceToken, singular, plural, clientResource.endpointPath());
    }

    private ApiAuthMode authMode(ApiEndpointModel endpoint) {
        if (endpoint.method() == HttpMethod.GET || endpoint.method() == HttpMethod.HEAD || endpoint.method() == HttpMethod.OPTIONS) {
            return endpoint.authRequirements().isEmpty() ? ApiAuthMode.NONE : ApiAuthMode.AUTHORIZED;
        }
        return ApiAuthMode.AUTHORIZED;
    }

    private String requestDtoClass(String singularName, HttpMethod method) {
        return switch (method) {
            case POST -> "Create" + singularName + "Request";
            case PUT -> "Update" + singularName + "Request";
            case PATCH -> "Patch" + singularName + "Request";
            default -> "";
        };
    }

    private String responseDtoClass(String singularName) {
        return singularName + "Response";
    }

    private List<ApiDtoSpec> dtoSpecs(ApiResourceDescriptor clientResource, List<ApiEndpointModel> endpoints) {
        List<ApiDtoSpec> specs = new ArrayList<>();
        for (ApiEndpointModel endpoint : endpoints) {
            ApiResourceDescriptor endpointResource = endpointResourceDescriptor(clientResource, endpoint);
            specs.add(new ApiDtoSpec(
                    RESPONSE_PACKAGE,
                    responseDtoClass(endpointResource.singularName()),
                    ApiDtoKind.RESPONSE,
                    fieldCatalog.fields(endpointResource.resourceToken(), ApiDtoKind.RESPONSE)
            ));
            String requestDtoClass = requestDtoClass(endpointResource.singularName(), endpoint.method());
            if (requestDtoClass.isBlank()) {
                continue;
            }
            ApiDtoKind kind = dtoKind(endpoint.method());
            specs.add(new ApiDtoSpec(
                    dtoPackage(kind),
                    requestDtoClass,
                    kind,
                    fieldCatalog.fields(endpointResource.resourceToken(), kind)
            ));
        }
        return specs;
    }

    private ApiDtoKind dtoKind(HttpMethod method) {
        return switch (method) {
            case PUT -> ApiDtoKind.UPDATE_REQUEST;
            case PATCH -> ApiDtoKind.PATCH_REQUEST;
            default -> ApiDtoKind.CREATE_REQUEST;
        };
    }

    private String dtoPackage(ApiDtoKind kind) {
        return switch (kind) {
            case UPDATE_REQUEST -> UPDATE_PACKAGE;
            case RESPONSE -> RESPONSE_PACKAGE;
            default -> REQUEST_PACKAGE;
        };
    }

    private List<ApiDtoSpec> deduplicateDtos(List<ApiDtoSpec> specs) {
        Map<String, ApiDtoSpec> deduped = new LinkedHashMap<>();
        specs.forEach(spec -> deduped.putIfAbsent(spec.packageName() + "." + spec.className(), spec));
        return new ArrayList<>(deduped.values());
    }

    private List<ApiTestSpec> testSpecs(
            CanonicalApiTestCaseBundle testCaseBundle,
            Map<String, ApiClientSpec> clientsByEndpointId
    ) {
        if (testCaseBundle == null) {
            return List.of();
        }
        List<ApiTestSpec> specs = new ArrayList<>();
        for (CanonicalApiTestCase testCase : testCaseBundle.testCases()) {
            ApiClientSpec client = clientsByEndpointId.get(testCase.endpointId());
            if (client == null) {
                continue;
            }
            ApiClientMethodSpec method = client.methods().stream()
                    .filter(candidate -> candidate.httpMethod() == testCase.method())
                    .filter(candidate -> samePath(candidate.pathTemplate(), testCase.path()))
                    .filter(candidate -> candidate.authMode() != ApiAuthMode.UNAUTHORIZED)
                    .findFirst()
                    .orElse(null);
            if (method == null) {
                continue;
            }
            specs.add(new ApiTestSpec(
                    TEST_PACKAGE,
                    toClassName(testCase.id()) + client.className() + "Test",
                    toMethodName(testCase.title()),
                    client.className(),
                    method.methodName(),
                    method.requestDtoClassName(),
                    method.responseDtoClassName(),
                    testCase.dataSetName(),
                    testCase.assertionContract()
            ));
        }
        return specs;
    }

    private boolean samePath(String left, String right) {
        return normalizePath(left).equals(normalizePath(right));
    }

    private String methodName(ApiResourceDescriptor resource, ApiEndpointModel endpoint, List<String> pathParameters) {
        if (hasNestedResourceAfterPathParameter(endpoint.path()) && !safe(endpoint.operationName()).isBlank()) {
            return toLowerCamel(endpoint.operationName());
        }
        return methodName(resource.singularName(), resource.resourceToken(), endpoint.method(), pathParameters);
    }

    private String methodName(String singularName, String resourceToken, HttpMethod method, List<String> pathParameters) {
        boolean byId = !pathParameters.isEmpty();
        String pluralName = toClassName(resourceToken);
        return switch (method) {
            case GET -> byId ? "get" + singularName + "ById" : "get" + pluralName;
            case POST -> "create" + singularName;
            case PUT -> "update" + singularName + "ById";
            case PATCH -> "patch" + singularName + "ById";
            case DELETE -> "delete" + singularName + "ById";
            default -> method.name().toLowerCase(Locale.ROOT) + singularName;
        };
    }

    private boolean hasNestedResourceAfterPathParameter(String path) {
        boolean seenPathParameter = false;
        for (String part : normalizePath(path).split("/")) {
            if (part.isBlank()) {
                continue;
            }
            if (part.startsWith("{") && part.endsWith("}")) {
                seenPathParameter = true;
                continue;
            }
            if (seenPathParameter) {
                return true;
            }
        }
        return false;
    }

    private String baseEndpointPath(String path) {
        String normalized = normalizePath(path);
        String[] parts = normalized.split("/");
        List<String> output = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (part.startsWith("{") && part.endsWith("}")) {
                break;
            }
            output.add(part);
        }
        if (output.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", output);
    }

    private String resourceToken(String endpointPath) {
        String[] parts = normalizePath(endpointPath).split("/");
        for (int index = parts.length - 1; index >= 0; index--) {
            if (!parts[index].isBlank()) {
                return parts[index];
            }
        }
        return "resource";
    }

    private String terminalResourceToken(String path) {
        String[] parts = normalizePath(path).split("/");
        for (int index = parts.length - 1; index >= 0; index--) {
            String part = parts[index];
            if (part.isBlank() || part.startsWith("{") && part.endsWith("}")) {
                continue;
            }
            return part;
        }
        return "";
    }

    private List<String> pathParameters(String path) {
        Set<String> params = new LinkedHashSet<>();
        for (String part : normalizePath(path).split("/")) {
            if (part.startsWith("{") && part.endsWith("}") && part.length() > 2) {
                params.add(part.substring(1, part.length() - 1));
            }
        }
        return new ArrayList<>(params);
    }

    private String singularize(String value) {
        String normalized = value == null ? "resource" : value.trim();
        if (normalized.endsWith("ies")) {
            return normalized.substring(0, normalized.length() - 3) + "y";
        }
        if (normalized.endsWith("s") && normalized.length() > 1) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String toClassName(String value) {
        StringBuilder builder = new StringBuilder();
        for (String token : safe(value).split("[^A-Za-z0-9]+")) {
            if (token.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }
        return builder.isEmpty() ? "GeneratedApi" : builder.toString();
    }

    private String toConstantToken(String value) {
        String normalized = safe(value).replaceAll("([a-z])([A-Z])", "$1_$2");
        return normalized.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("(^_+|_+$)", "");
    }

    private String toMethodName(String value) {
        String className = toClassName(value);
        return "should" + className;
    }

    private String toLowerCamel(String value) {
        String className = toClassName(value);
        if (className.isBlank()) {
            return "generatedApiCall";
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private String normalizePath(String path) {
        String normalized = safe(path);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
