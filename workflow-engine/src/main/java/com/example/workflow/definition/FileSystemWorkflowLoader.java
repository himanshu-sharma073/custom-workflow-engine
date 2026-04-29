package com.example.workflow.definition;

import com.example.workflow.dsl.WorkflowDefinition;
import com.example.workflow.dsl.WorkflowDefinitionParser;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

public class FileSystemWorkflowLoader implements WorkflowDefinitionLoader {
    private final String basePath;
    private final WorkflowDefinitionParser parser;

    public FileSystemWorkflowLoader(String basePath, WorkflowDefinitionParser parser) {
        this.basePath = basePath;
        this.parser = parser;
    }

    @Override
    public WorkflowDefinition load(String workflowId) {
        String resolvedId = resolveWorkflowId(workflowId);
        FileSystemResource resource = new FileSystemResource(basePath + "/" + resolvedId + ".json");
        if (!resource.exists()) {
            throw new IllegalArgumentException("Workflow definition not found: " + workflowId + " at " + basePath);
        }
        try (var is = resource.getInputStream()) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load workflow definition " + workflowId, e);
        }
    }

    private String resolveWorkflowId(String workflowId) {
        if ("document-update-approval".equals(workflowId)) {
            return "ratings-review-workflow";
        }
        return workflowId;
    }
}
