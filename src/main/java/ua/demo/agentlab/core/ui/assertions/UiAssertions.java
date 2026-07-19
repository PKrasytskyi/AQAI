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

    public static void assertEquals(Object actual, Object expected, String message) {
        Assert.assertEquals(actual, expected, message);
    }

    public static void assertContains(String actual, String expectedFragment, String message) {
        Assert.assertTrue(
                actual != null && actual.contains(expectedFragment),
                message
        );
    }
}
