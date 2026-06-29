package ua.demo.agentlab.mcp.service;

import org.junit.jupiter.api.Test;
import ua.demo.agentlab.mcp.model.SourceKind;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementTextNormalizerTest {

    private final RequirementTextNormalizer normalizer = new RequirementTextNormalizer();

    @Test
    void extractsHeadingAndAcceptanceCriteria() {
        var bundle = normalizer.normalize("""
                # Login

                A registered user can sign in.

                - Valid credentials allow access.
                - Five failed attempts lock the account.
                """, "BA-12", null, SourceKind.BA_REQUIREMENT);

        assertThat(bundle.source().id()).isEqualTo("BA-12");
        assertThat(bundle.requirements()).singleElement().satisfies(requirement -> {
            assertThat(requirement.title()).isEqualTo("Login");
            assertThat(requirement.acceptanceCriteria()).containsExactly(
                    "Valid credentials allow access.",
                    "Five failed attempts lock the account."
            );
        });
        assertThat(bundle.warnings()).isEmpty();
    }
}
