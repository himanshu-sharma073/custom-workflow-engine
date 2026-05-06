package com.example.workflow.persistence.spi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Append-only history entry for a workflow step transition")
public record WorkflowHistoryRecord(
    String workflowId,
    String stepId,
    String status,
    @Schema(description = "Serialized payload / outcome")
    String payload,
    Instant createdAt
) {}
