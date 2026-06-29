package ua.demo.agentlab.core.api.assertions;

import org.testng.Assert;

public final class ApiAssertions {

    private ApiAssertions() {
    }

    public static void assertStatusCode(int actualStatusCode, int expectedStatusCode) {
        Assert.assertEquals(
                actualStatusCode,
                expectedStatusCode,
                "Unexpected API status code"
        );
    }

    public static void assertFieldEquals(String actualValue, String expectedValue, String fieldName) {
        Assert.assertEquals(
                actualValue,
                expectedValue,
                "Unexpected API field value for: " + fieldName
        );
    }
}
