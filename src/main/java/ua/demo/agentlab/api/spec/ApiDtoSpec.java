package ua.demo.agentlab.api.spec;

import java.util.List;

public record ApiDtoSpec(
        String packageName,
        String className,
        ApiDtoKind kind,
        List<ApiDtoFieldSpec> fields
) {
    public ApiDtoSpec {
        packageName = packageName == null || packageName.isBlank()
                ? "ua.demo.agentlab.api.generated.models"
                : packageName.trim();
        className = className == null ? "" : className.trim();
        kind = kind == null ? ApiDtoKind.RESPONSE : kind;
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
