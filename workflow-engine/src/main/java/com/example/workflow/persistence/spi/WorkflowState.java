package com.example.workflow.persistence.spi;

import java.time.Instant;
import java.util.Map;

public record WorkflowState(String workflowId, String definitionId, String status, String currentStep, Map<String, Object> context, Instant updatedAt) {}
