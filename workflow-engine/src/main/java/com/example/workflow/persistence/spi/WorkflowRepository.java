package com.example.workflow.persistence.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WorkflowRepository {
    WorkflowState create(String definitionId, Map<String, Object> context);
    Optional<WorkflowState> find(String workflowId);
    List<WorkflowState> findAll();
    void updateState(String workflowId, String status, String currentStep, Map<String, Object> context);
}
