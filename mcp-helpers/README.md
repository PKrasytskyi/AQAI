# Automation Test Helpers MCP Server

This module exposes deterministic intake helpers through Model Context Protocol (MCP).
It is intentionally separate from the main orchestration runtime.

## Current tools

- `requirements_normalize_text`
  - Converts BA, markdown, or raw requirement text into a canonical requirement bundle.
- `jira_normalize_story`
  - Converts an already fetched Jira story into the same canonical requirement bundle.
- `csv_parse_test_cases`
  - Converts CSV test cases into canonical test case objects.

The Jira tool does not fetch Jira directly yet. Authentication and remote retrieval will be
added as a separate connector layer. This keeps normalization deterministic and easy to test.

## Build

```powershell
mvn --batch-mode test
mvn --batch-mode package
```

Run the commands from the `mcp-helpers` directory.

## Run as a STDIO MCP server

```powershell
java -jar target/agentlab-mcp-helpers-0.1.0-SNAPSHOT.jar
```

STDIO is reserved for MCP JSON-RPC traffic, so application logging is disabled in
`application.properties`.

## Example MCP host configuration

```json
{
  "mcpServers": {
    "automation-test-helpers": {
      "command": "java",
      "args": [
        "-jar",
        "C:/absolute/path/agent_base_testing_framework/mcp-helpers/target/agentlab-mcp-helpers-0.1.0-SNAPSHOT.jar"
      ]
    }
  }
}
```

## Design boundary

The MCP server owns external format normalization and connector helpers.
The main Java orchestration project owns planning, Selenium/TestNG generation,
validation, review, and persistence.
