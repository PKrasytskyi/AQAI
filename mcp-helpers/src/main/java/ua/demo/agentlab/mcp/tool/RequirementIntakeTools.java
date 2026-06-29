package ua.demo.agentlab.mcp.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import ua.demo.agentlab.mcp.model.CanonicalRequirementBundle;
import ua.demo.agentlab.mcp.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.mcp.model.JiraStoryInput;
import ua.demo.agentlab.mcp.model.SourceKind;
import ua.demo.agentlab.mcp.service.CsvTestCaseParser;
import ua.demo.agentlab.mcp.service.JiraStoryNormalizer;
import ua.demo.agentlab.mcp.service.RequirementTextNormalizer;

@Component
public class RequirementIntakeTools {

    private final RequirementTextNormalizer textNormalizer;
    private final JiraStoryNormalizer jiraStoryNormalizer;
    private final CsvTestCaseParser csvTestCaseParser;

    public RequirementIntakeTools(
            RequirementTextNormalizer textNormalizer,
            JiraStoryNormalizer jiraStoryNormalizer,
            CsvTestCaseParser csvTestCaseParser
    ) {
        this.textNormalizer = textNormalizer;
        this.jiraStoryNormalizer = jiraStoryNormalizer;
        this.csvTestCaseParser = csvTestCaseParser;
    }

    @Tool(
            name = "requirements_normalize_text",
            description = "Normalize BA requirements, markdown, or raw requirement text into a canonical requirement bundle."
    )
    public CanonicalRequirementBundle normalizeRequirementText(
            @ToolParam(description = "Requirement text. Markdown headings and bullet lists are supported.") String text,
            @ToolParam(description = "Stable source identifier, for example BA-REQ-12.", required = false) String sourceId,
            @ToolParam(description = "Optional requirement title.", required = false) String title,
            @ToolParam(description = "Source kind: BA_REQUIREMENT or RAW_TEXT.", required = false) SourceKind sourceKind
    ) {
        return textNormalizer.normalize(text, sourceId, title, sourceKind);
    }

    @Tool(
            name = "jira_normalize_story",
            description = "Normalize an already fetched Jira user story into the canonical requirement bundle used by automation planning."
    )
    public CanonicalRequirementBundle normalizeJiraStory(
            @ToolParam(description = "Structured Jira story payload.") JiraStoryInput story
    ) {
        return jiraStoryNormalizer.normalize(story);
    }

    @Tool(
            name = "csv_parse_test_cases",
            description = "Parse CSV test cases into canonical test case objects. Expected headers include id, title, preconditions, steps, expected result, priority, and labels."
    )
    public CanonicalTestCaseBundle parseCsvTestCases(
            @ToolParam(description = "Full CSV content. Pass content, not a local file path.") String csvContent,
            @ToolParam(description = "Stable source identifier or CSV file name.", required = false) String sourceId,
            @ToolParam(description = "Delimiter: comma, semicolon, tab, or a single character.", required = false) String delimiter
    ) {
        return csvTestCaseParser.parse(csvContent, sourceId, delimiter);
    }
}
