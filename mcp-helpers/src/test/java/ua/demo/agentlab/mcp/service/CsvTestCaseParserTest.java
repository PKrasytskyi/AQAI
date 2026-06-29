package ua.demo.agentlab.mcp.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvTestCaseParserTest {

    private final CsvTestCaseParser parser = new CsvTestCaseParser();

    @Test
    void parsesCommonTestCaseColumns() {
        var bundle = parser.parse("""
                id,title,preconditions,steps,expected result,priority,tags
                TC-1,Valid login,Registered user,Open login|Enter credentials|Submit,Dashboard is displayed,High,smoke;ui
                """, "login-cases.csv", "comma");

        assertThat(bundle.testCases()).singleElement().satisfies(testCase -> {
            assertThat(testCase.id()).isEqualTo("TC-1");
            assertThat(testCase.steps()).containsExactly(
                    "Open login",
                    "Enter credentials",
                    "Submit"
            );
            assertThat(testCase.labels()).containsExactly("smoke", "ui");
        });
    }
}
