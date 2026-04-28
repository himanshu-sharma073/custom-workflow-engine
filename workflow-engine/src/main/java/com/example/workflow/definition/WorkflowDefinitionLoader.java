package com.example.workflow.definition;

import com.example.workflow.dsl.WorkflowDefinition;

public interface WorkflowDefinitionLoader {
    WorkflowDefinition load(String workflowId);
}
