package ua.demo.agentlab.config;

public interface ProjectProfileLoader {

    ProjectProfile loadDefaultProfile();

    default String defaultRequirementLocation() {
        return "";
    }
}
