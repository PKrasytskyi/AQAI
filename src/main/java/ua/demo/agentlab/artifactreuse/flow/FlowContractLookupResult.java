package ua.demo.agentlab.artifactreuse.flow;

import java.util.List;

/** Explicit result for exact flow lookups so an empty match is never confused with a backend failure. */
public record FlowContractLookupResult(
        boolean attempted,
        boolean success,
        List<FlowContract> contracts,
        String message
) {
    public FlowContractLookupResult {
        contracts = contracts == null ? List.of() : List.copyOf(contracts);
        message = message == null ? "" : message.trim();
    }

    public static FlowContractLookupResult skipped(String message) {
        return new FlowContractLookupResult(false, false, List.of(), message);
    }

    public static FlowContractLookupResult success(List<FlowContract> contracts, String message) {
        return new FlowContractLookupResult(true, true, contracts, message);
    }

    public static FlowContractLookupResult failed(String message) {
        return new FlowContractLookupResult(true, false, List.of(), message);
    }
}
