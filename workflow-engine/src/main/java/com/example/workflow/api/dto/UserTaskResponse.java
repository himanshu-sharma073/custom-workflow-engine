package com.example.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "User task surfaced to API clients")
public record UserTaskResponse(
    @Schema(description = "Primary key")
    UUID id,
    @Schema(description = "Owning workflow instance")
    String workflowId,
    @Schema(description = "Step definition id")
    String stepId,
    @Schema(example = "OPEN")
    String status,
    @Schema(description = "How the task is assigned", example = "ROLE")
    String assignmentType,
    String assignmentValue,
    @Schema(description = "User id if claimed")
    String claimedBy,
    Instant createdAt
) {}
