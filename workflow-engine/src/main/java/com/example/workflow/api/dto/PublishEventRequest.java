package com.example.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "Inbound event for correlation with a waiting workflow step")
public record PublishEventRequest(
    @Schema(description = "Logical event name configured on the step", example = "document-approved")
    @NotBlank String eventName,
    @Schema(description = "Correlation id to match in-flight work", example = "corr-001")
    @NotBlank String correlationId,
    @Schema(description = "Opaque JSON payload forwarded to the engine")
    Map<String, Object> payload
) {}
