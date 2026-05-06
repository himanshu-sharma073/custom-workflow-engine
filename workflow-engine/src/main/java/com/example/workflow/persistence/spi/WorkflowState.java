package com.example.workflow.persistence.spi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Serializable snapshot of a workflow instance")
public record WorkflowState(
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    String workflowId,
    @Schema(example = "ratings-review-workflow")
    String definitionId,
    @Schema(example = "RUNNING")
    String status,
    @Schema(description = "Current step id")
    String currentStep,
    @Schema(description = "Merged variables and step outputs")
    Map<String, Object> context,
    Instant updatedAt
) {}
