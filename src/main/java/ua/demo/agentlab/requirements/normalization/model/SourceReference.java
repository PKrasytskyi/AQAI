package ua.demo.agentlab.requirements.normalization.model;

public record SourceReference(

        String source,
        int startLine,
        int endLine,
        String fragment
) {}
