package ua.demo.agentlab.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ua.demo.agentlab.mcp.tool.RequirementIntakeTools;

@SpringBootApplication
public class AutomationHelpersMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutomationHelpersMcpApplication.class, args);
    }

    @Bean
    ToolCallbackProvider requirementIntakeToolProvider(RequirementIntakeTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
