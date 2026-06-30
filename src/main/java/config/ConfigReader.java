package config;

/**
 * @deprecated Use {@link ua.demo.agentlab.core.config.ConfigReader}. This facade
 * exists only for older generated examples that imported {@code config.ConfigReader}.
 */
@Deprecated(since = "0.0.1", forRemoval = false)
public final class ConfigReader {

    private ConfigReader() {
    }

    public static String getBaseUrl() {
        return ua.demo.agentlab.core.config.ConfigReader.getBaseUrl();
    }

    public static String getApiBaseUrl() {
        return ua.demo.agentlab.core.config.ConfigReader.getApiBaseUrl();
    }

    public static String getApiAuthToken() {
        return ua.demo.agentlab.core.config.ConfigReader.getApiAuthToken();
    }

    public static long getTimeout() {
        return ua.demo.agentlab.core.config.ConfigReader.getTimeout();
    }

    public static String getBrowser() {
        return ua.demo.agentlab.core.config.ConfigReader.getBrowser();
    }

    public static boolean isHeadless() {
        return ua.demo.agentlab.core.config.ConfigReader.isHeadless();
    }

    public static String getValidUsername() {
        return ua.demo.agentlab.core.config.ConfigReader.getValidUsername();
    }

    public static String getValidPassword() {
        return ua.demo.agentlab.core.config.ConfigReader.getValidPassword();
    }

    public static String getInvalidUsername() {
        return ua.demo.agentlab.core.config.ConfigReader.getInvalidUsername();
    }

    public static String getInvalidPassword() {
        return ua.demo.agentlab.core.config.ConfigReader.getInvalidPassword();
    }

    public static String getRegistrationUsernamePrefix() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationUsernamePrefix();
    }

    public static String getRegistrationPassword() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationPassword();
    }

    public static String getRegistrationFirstName() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationFirstName();
    }

    public static String getRegistrationLastName() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationLastName();
    }

    public static String getRegistrationAddress() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationAddress();
    }

    public static String getRegistrationCity() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationCity();
    }

    public static String getRegistrationState() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationState();
    }

    public static String getRegistrationZipCode() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationZipCode();
    }

    public static String getRegistrationPhone() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationPhone();
    }

    public static String getRegistrationSsn() {
        return ua.demo.agentlab.core.config.ConfigReader.getRegistrationSsn();
    }

    public static String getBillPayName() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayName();
    }

    public static String getBillPayAddress() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayAddress();
    }

    public static String getBillPayCity() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayCity();
    }

    public static String getBillPayState() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayState();
    }

    public static String getBillPayZipCode() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayZipCode();
    }

    public static String getBillPayPhone() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayPhone();
    }

    public static String getBillPayAccountNumber() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayAccountNumber();
    }

    public static String getBillPayVerifyAccountNumber() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayVerifyAccountNumber();
    }

    public static String getBillPayAmount() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayAmount();
    }

    public static int getBillPayFromAccountIndex() {
        return ua.demo.agentlab.core.config.ConfigReader.getBillPayFromAccountIndex();
    }

    public static String getTransferAmount() {
        return ua.demo.agentlab.core.config.ConfigReader.getTransferAmount();
    }

    public static int getTransferFromAccountIndex() {
        return ua.demo.agentlab.core.config.ConfigReader.getTransferFromAccountIndex();
    }

    public static int getTransferToAccountIndex() {
        return ua.demo.agentlab.core.config.ConfigReader.getTransferToAccountIndex();
    }
}
