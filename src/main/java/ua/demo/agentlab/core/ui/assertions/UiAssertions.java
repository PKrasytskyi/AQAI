package ua.demo.agentlab.core.ui.assertions;

import org.testng.Assert;

public final class UiAssertions {

    private UiAssertions() {
    }

    public static void assertTrue(boolean condition, String message) {
        Assert.assertTrue(condition, message);
    }

    public static void assertFalse(boolean condition, String message) {
        Assert.assertFalse(condition, message);
    }

    public static void assertUrlContains(String actualUrl, String expectedFragment) {
        Assert.assertTrue(
                actualUrl != null && actualUrl.contains(expectedFragment),
                "Expected current URL to contain: " + expectedFragment
        );
    }
}
