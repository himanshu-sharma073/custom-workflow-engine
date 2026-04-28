package com.example.workflow.definition;

import com.example.workflow.dsl.WorkflowDefinition;
import com.example.workflow.dsl.WorkflowDefinitionParser;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

public class ClasspathWorkflowLoader implements WorkflowDefinitionLoader {
    private final String basePath;
    private final WorkflowDefinitionParser parser;

    public ClasspathWorkflowLoader(String basePath, WorkflowDefinitionParser parser) {
        this.basePath = basePath;
        this.parser = parser;
    }

    @Override
    public WorkflowDefinition load(String workflowId) {
        ClassPathResource resource = new ClassPathResource(basePath + "/" + workflowId + ".json");
        if (!resource.exists()) {
            throw new IllegalArgumentException("Workflow definition not found: " + workflowId + " at classpath:" + basePath);
        }
        try (var is = resource.getInputStream()) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load workflow definition " + workflowId, e);
        }
    }
}
