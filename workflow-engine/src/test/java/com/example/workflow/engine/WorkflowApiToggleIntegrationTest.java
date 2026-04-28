package com.example.workflow.engine;

import com.example.workflow.api.TaskController;
import com.example.workflow.api.WorkflowEngineController;
import com.example.workflow.autoconfigure.WorkflowEngineAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowApiToggleIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(WorkflowEngineAutoConfiguration.class))
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:testdb",
            "spring.datasource.driverClassName=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=none"
        );

    @Test
    void apiControllersDisabledWhenPropertyFalse() {
        contextRunner
            .withPropertyValues("workflow.engine.api.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(TaskController.class);
                assertThat(context).doesNotHaveBean(WorkflowEngineController.class);
            });
    }
}
