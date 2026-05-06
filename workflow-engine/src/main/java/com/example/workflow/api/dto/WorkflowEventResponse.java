package com.example.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Persisted audit row for a workflow event")
public record WorkflowEventResponse(
    @Schema(description = "Event type / name")
    String eventType,
    @Schema(description = "Serialized payload snapshot")
    String payload,
    @Schema(description = "UTC creation time")
    Instant createdAt
) {}
