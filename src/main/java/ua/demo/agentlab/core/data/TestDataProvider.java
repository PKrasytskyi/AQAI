package ua.demo.agentlab.core.data;

public interface TestDataProvider {

    String getBaseUrl();

    UserCredentials credentials(String profileName);

    ScenarioData scenarioData(String dataSetName);
}
