package ua.demo.agentlab.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {

    private static final String CONFIG_RESOURCE = "framework.properties";
    private static final Properties PROPERTIES = loadProperties();

    private ConfigReader() {
    }

    public static String getBaseUrl() {
        return readText("test.base-url", "TEST_BASE_URL", "http://localhost:8080");
    }

    public static String getApiBaseUrl() {
        return readText(
                "api.base-url",
                "API_BASE_URL",
                readText("project.api.base-url", "PROJECT_API_BASE_URL", getBaseUrl())
        );
    }

    public static String getApiAuthToken() {
        return readText(
                "api.auth.token",
                "API_AUTH_TOKEN",
                readText("project.api.auth.token", "PROJECT_API_AUTH_TOKEN", "")
        );
    }

    public static long getTimeout() {
        String rawValue = readText("test.timeout-seconds", "TEST_TIMEOUT_SECONDS", "10");
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid timeout value: " + rawValue, exception);
        }
    }

    public static String getBrowser() {
        return readText("test.browser", "TEST_BROWSER", "chrome").toLowerCase();
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(readText("test.headless", "TEST_HEADLESS", "false"));
    }

    public static String getValidUsername() {
        return readText("test.credentials.valid.username", "TEST_VALID_USERNAME", "john");
    }

    public static String getValidPassword() {
        return readText("test.credentials.valid.password", "TEST_VALID_PASSWORD", "demo");
    }

    public static String getInvalidUsername() {
        return readText("test.credentials.invalid.username", "TEST_INVALID_USERNAME", "wrong-user");
    }

    public static String getInvalidPassword() {
        return readText("test.credentials.invalid.password", "TEST_INVALID_PASSWORD", "wrong-password");
    }

    public static String getRegistrationUsernamePrefix() {
        return readText(
                "test.credentials.registration.username-prefix",
                "TEST_REGISTRATION_USERNAME_PREFIX",
                "autouser"
        );
    }

    public static String getRegistrationPassword() {
        return readText(
                "test.credentials.registration.password",
                "TEST_REGISTRATION_PASSWORD",
                getValidPassword()
        );
    }

    public static String getRegistrationFirstName() {
        return readText("test.data.registration.first-name", "TEST_REGISTRATION_FIRST_NAME", "John");
    }

    public static String getRegistrationLastName() {
        return readText("test.data.registration.last-name", "TEST_REGISTRATION_LAST_NAME", "Doe");
    }

    public static String getRegistrationAddress() {
        return readText("test.data.registration.address", "TEST_REGISTRATION_ADDRESS", "Main Street 1");
    }

    public static String getRegistrationCity() {
        return readText("test.data.registration.city", "TEST_REGISTRATION_CITY", "Testville");
    }

    public static String getRegistrationState() {
        return readText("test.data.registration.state", "TEST_REGISTRATION_STATE", "CA");
    }

    public static String getRegistrationZipCode() {
        return readText("test.data.registration.zip-code", "TEST_REGISTRATION_ZIP_CODE", "90210");
    }

    public static String getRegistrationPhone() {
        return readText("test.data.registration.phone", "TEST_REGISTRATION_PHONE", "1234567890");
    }

    public static String getRegistrationSsn() {
        return readText("test.data.registration.ssn", "TEST_REGISTRATION_SSN", "123-45-6789");
    }

    public static String getBillPayName() {
        return readText("test.data.bill-pay.name", "TEST_BILL_PAY_NAME", "Utility Company");
    }

    public static String getBillPayAddress() {
        return readText("test.data.bill-pay.address", "TEST_BILL_PAY_ADDRESS", "Payment Street 5");
    }

    public static String getBillPayCity() {
        return readText("test.data.bill-pay.city", "TEST_BILL_PAY_CITY", "Billtown");
    }

    public static String getBillPayState() {
        return readText("test.data.bill-pay.state", "TEST_BILL_PAY_STATE", "TX");
    }

    public static String getBillPayZipCode() {
        return readText("test.data.bill-pay.zip-code", "TEST_BILL_PAY_ZIP_CODE", "73301");
    }

    public static String getBillPayPhone() {
        return readText("test.data.bill-pay.phone", "TEST_BILL_PAY_PHONE", "1234567890");
    }

    public static String getBillPayAccountNumber() {
        return readText("test.data.bill-pay.account-number", "TEST_BILL_PAY_ACCOUNT_NUMBER", "123456789");
    }

    public static String getBillPayVerifyAccountNumber() {
        return readText(
                "test.data.bill-pay.verify-account-number",
                "TEST_BILL_PAY_VERIFY_ACCOUNT_NUMBER",
                getBillPayAccountNumber()
        );
    }

    public static String getBillPayAmount() {
        return readText("test.data.bill-pay.amount", "TEST_BILL_PAY_AMOUNT", "35");
    }

    public static int getBillPayFromAccountIndex() {
        return readInt("test.data.bill-pay.from-account-index", "TEST_BILL_PAY_FROM_ACCOUNT_INDEX", 0);
    }

    public static String getTransferAmount() {
        return readText("test.data.transfer.amount", "TEST_TRANSFER_AMOUNT", "25");
    }

    public static int getTransferFromAccountIndex() {
        return readInt("test.data.transfer.from-account-index", "TEST_TRANSFER_FROM_ACCOUNT_INDEX", 0);
    }

    public static int getTransferToAccountIndex() {
        return readInt("test.data.transfer.to-account-index", "TEST_TRANSFER_TO_ACCOUNT_INDEX", 1);
    }

    private static String readText(String propertyKey, String envKey, String defaultValue) {
        String systemValue = System.getProperty(propertyKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return resolvePlaceholders(systemValue.trim());
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return resolvePlaceholders(envValue.trim());
        }

        String propertyValue = PROPERTIES.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return resolvePlaceholders(propertyValue.trim());
        }

        return resolvePlaceholders(defaultValue);
    }

    private static String resolvePlaceholders(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String resolved = value;
        for (int index = 0; index < 8; index++) {
            int start = resolved.indexOf("${");
            if (start < 0) {
                return resolved;
            }
            int end = resolved.indexOf('}', start);
            if (end < 0) {
                return resolved;
            }
            String key = resolved.substring(start + 2, end).trim();
            String replacement = readRawValue(key);
            if (replacement == null) {
                replacement = "";
            }
            resolved = resolved.substring(0, start) + replacement + resolved.substring(end + 1);
        }
        return resolved;
    }

    private static String readRawValue(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = PROPERTIES.getProperty(key);
        return propertyValue == null || propertyValue.isBlank() ? null : propertyValue.trim();
    }

    private static int readInt(String propertyKey, String envKey, int defaultValue) {
        String rawValue = readText(propertyKey, envKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid integer value for " + propertyKey + ": " + rawValue, exception);
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load " + CONFIG_RESOURCE, exception);
        }

        return properties;
    }
}
