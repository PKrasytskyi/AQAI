package ua.demo.agentlab.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ua.demo.agentlab")
public class AgentOrchestratorLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentOrchestratorLabApplication.class, args);
    }
}
