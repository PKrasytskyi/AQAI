package ua.demo.agentlab.mcp.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import ua.demo.agentlab.mcp.model.CanonicalTestCase;
import ua.demo.agentlab.mcp.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.mcp.model.SourceDescriptor;
import ua.demo.agentlab.mcp.model.SourceKind;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CsvTestCaseParser {

    public CanonicalTestCaseBundle parse(String csvContent, String sourceId, String delimiter) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new IllegalArgumentException("CSV content is required.");
        }

        char delimiterCharacter = resolveDelimiter(delimiter);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiterCharacter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .get();

        List<CanonicalTestCase> testCases = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (CSVParser parser = format.parse(new StringReader(csvContent))) {
            Map<String, String> headers = normalizeHeaders(parser.getHeaderMap());
            int rowNumber = 1;

            for (CSVRecord record : parser) {
                rowNumber++;
                String title = value(record, headers, "title", "name", "testcase", "test case");
                if (title.isBlank()) {
                    warnings.add("Skipped CSV row " + rowNumber + " because title is empty.");
                    continue;
                }

                String id = value(record, headers, "id", "caseid", "case id", "testcaseid");
                if (id.isBlank()) {
                    id = "CSV-" + (testCases.size() + 1);
                }

                testCases.add(new CanonicalTestCase(
                        id,
                        title,
                        value(record, headers, "preconditions", "precondition"),
                        splitList(value(record, headers, "steps", "test steps", "actions")),
                        value(record, headers, "expectedresult", "expected result", "expected", "result"),
                        value(record, headers, "priority"),
                        splitLabels(value(record, headers, "labels", "tags"))
                ));
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot parse CSV content.", exception);
        }

        SourceDescriptor source = new SourceDescriptor(
                SourceKind.CSV_TEST_CASES,
                sourceId == null || sourceId.isBlank() ? "csv-input" : sourceId.trim(),
                "CSV test cases",
                null
        );

        return new CanonicalTestCaseBundle(source, testCases, warnings);
    }

    private Map<String, String> normalizeHeaders(Map<String, Integer> headerMap) {
        return headerMap.keySet().stream()
                .collect(Collectors.toMap(
                        this::normalizeHeader,
                        header -> header,
                        (first, ignored) -> first
                ));
    }

    private String value(CSVRecord record, Map<String, String> headers, String... aliases) {
        for (String alias : aliases) {
            String originalHeader = headers.get(normalizeHeader(alias));
            if (originalHeader != null && record.isMapped(originalHeader)) {
                String value = record.get(originalHeader);
                return value == null ? "" : value.trim();
            }
        }
        return "";
    }

    private String normalizeHeader(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\r?\\n|\\|"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private List<String> splitLabels(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[,;|]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private char resolveDelimiter(String delimiter) {
        if (delimiter == null || delimiter.isBlank() || "comma".equalsIgnoreCase(delimiter)) {
            return ',';
        }
        if ("semicolon".equalsIgnoreCase(delimiter)) {
            return ';';
        }
        if ("tab".equalsIgnoreCase(delimiter) || "\\t".equals(delimiter)) {
            return '\t';
        }
        if (delimiter.length() == 1) {
            return delimiter.charAt(0);
        }
        throw new IllegalArgumentException("Delimiter must be comma, semicolon, tab, or one character.");
    }
}
