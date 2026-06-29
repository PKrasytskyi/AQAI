package ua.demo.agentlab.ui.discovery.persistence.knowledge.config;

public interface Neo4jRuntimeConfig {

    boolean enabled();

    String httpUrl();

    String database();

    String username();

    String password();
}
