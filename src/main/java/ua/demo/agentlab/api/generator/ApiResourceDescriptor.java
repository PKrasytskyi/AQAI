package ua.demo.agentlab.api.generator;

record ApiResourceDescriptor(
        String resourceToken,
        String singularName,
        String pluralName,
        String endpointPath
) {
}
