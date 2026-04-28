package com.example.workflow.engine;

import com.example.workflow.persistence.spi.WorkflowState;

import java.util.Map;

public interface WorkflowEngine {
    WorkflowState startWorkflow(String definitionId, Map<String, Object> input);
    WorkflowState resumeWorkflow(String workflowId, Map<String, Object> input);
    WorkflowState rollbackWorkflow(String workflowId);
    WorkflowState getWorkflow(String workflowId);
}
