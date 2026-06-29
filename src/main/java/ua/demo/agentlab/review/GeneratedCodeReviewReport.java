package ua.demo.agentlab.review;

import java.util.List;

public record GeneratedCodeReviewReport(

        String summary,
        int reviewedFiles,
        int totalFindings,
        List<GeneratedCodeReviewFinding> findings

        )
{
    public boolean hasFindings(){
        return !findings.isEmpty();
    }

}
