package com.example.workflow.definition;

import com.example.workflow.dsl.WorkflowDefinition;

public class DatabaseWorkflowLoader implements WorkflowDefinitionLoader {
    @Override
    public WorkflowDefinition load(String workflowId) {
        throw new UnsupportedOperationException("Database workflow loader is deferred in this iteration");
    }
}
