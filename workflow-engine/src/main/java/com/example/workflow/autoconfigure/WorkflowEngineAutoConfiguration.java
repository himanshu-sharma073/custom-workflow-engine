package com.example.workflow.autoconfigure;

import com.example.workflow.auth.NoopUserContextProvider;
import com.example.workflow.auth.UserContextProvider;
import com.example.workflow.bonus.LoggingNotificationPublisher;
import com.example.workflow.bonus.NotificationPublisher;
import com.example.workflow.definition.*;
import com.example.workflow.dsl.WorkflowDefinitionParser;
import com.example.workflow.engine.compensation.CompensationHandler;
import com.example.workflow.engine.compensation.NoopCompensationHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@AutoConfiguration
@ConditionalOnProperty(prefix = "workflow.engine", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.example.workflow")
@EntityScan(basePackages = "com.example.workflow.persistence.entity")
@EnableJpaRepositories(basePackages = "com.example.workflow.persistence.repository")
@EnableConfigurationProperties(WorkflowEngineProperties.class)
public class WorkflowEngineAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(UserContextProvider.class)
    public UserContextProvider userContextProvider() {
        return new NoopUserContextProvider();
    }

    @Bean
    @ConditionalOnMissingBean(NotificationPublisher.class)
    public NotificationPublisher notificationPublisher() {
        return new LoggingNotificationPublisher();
    }

    @Bean
    @ConditionalOnMissingBean(CompensationHandler.class)
    public CompensationHandler compensationHandler() {
        return new NoopCompensationHandler();
    }

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("workflow-engine-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowDefinitionLoader.class)
    public WorkflowDefinitionLoader workflowDefinitionLoader(WorkflowEngineProperties properties, WorkflowDefinitionParser parser) {
        WorkflowDefinitionLoader base = switch (properties.getDefinition().getSource().toLowerCase()) {
            case "file" -> new FileSystemWorkflowLoader(properties.getDefinition().getPath(), parser);
            case "db" -> new DatabaseWorkflowLoader();
            case "classpath" -> new ClasspathWorkflowLoader(properties.getDefinition().getPath(), parser);
            default -> throw new IllegalArgumentException("Unknown definition source: " + properties.getDefinition().getSource());
        };
        return new CachingWorkflowDefinitionLoader(base, properties.getDefinition().isCacheEnabled());
    }
}
