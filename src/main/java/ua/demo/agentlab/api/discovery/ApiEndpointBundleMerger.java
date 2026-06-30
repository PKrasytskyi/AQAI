package ua.demo.agentlab.api.discovery;

import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.ApiEndpointEvidence;
import ua.demo.agentlab.api.model.ApiEndpointModel;
import ua.demo.agentlab.api.model.ApiResponseModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class ApiEndpointBundleMerger {

    public ApiEndpointBundle merge(String source, List<ApiEndpointBundle> bundles) {
        Map<String, ApiEndpointModel> merged = new LinkedHashMap<>();
        if (bundles != null) {
            for (ApiEndpointBundle bundle : bundles) {
                if (bundle == null) {
                    continue;
                }
                for (ApiEndpointModel endpoint : bundle.endpoints()) {
                    merged.merge(endpoint.endpointId(), endpoint, this::mergeEndpoint);
                }
            }
        }
        return new ApiEndpointBundle(source, new ArrayList<>(merged.values()));
    }

    private ApiEndpointModel mergeEndpoint(ApiEndpointModel left, ApiEndpointModel right) {
        List<ApiResponseModel> responses = mergeByKey(left.responses(), right.responses(), response -> String.valueOf(response.statusCode()));
        List<ApiEndpointEvidence> evidence = mergeByKey(left.evidence(), right.evidence(), evidenceItem -> evidenceItem.source() + ":" + evidenceItem.reference());
        return new ApiEndpointModel(
                left.endpointId(),
                left.method(),
                left.path(),
                firstNonBlank(left.operationName(), right.operationName()),
                firstNonBlank(left.businessCapability(), right.businessCapability()),
                left.parameters().isEmpty() ? right.parameters() : left.parameters(),
                left.requestBody().present() ? left.requestBody() : right.requestBody(),
                responses,
                mergeStrings(left.authRequirements(), right.authRequirements()),
                evidence,
                Math.max(left.confidence(), right.confidence())
        );
    }

    private <T> List<T> mergeByKey(List<T> left, List<T> right, java.util.function.Function<T, String> keyFn) {
        Map<String, T> values = new LinkedHashMap<>();
        if (left != null) {
            left.forEach(value -> values.put(keyFn.apply(value), value));
        }
        if (right != null) {
            right.forEach(value -> values.putIfAbsent(keyFn.apply(value), value));
        }
        return new ArrayList<>(values.values());
    }

    private List<String> mergeStrings(List<String> left, List<String> right) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (left != null) {
            values.addAll(left);
        }
        if (right != null) {
            values.addAll(right);
        }
        return new ArrayList<>(values);
    }

    private String firstNonBlank(String first, String second) {
        String value = first == null ? "" : first.trim();
        return value.isBlank() ? second == null ? "" : second.trim() : value;
    }
}
