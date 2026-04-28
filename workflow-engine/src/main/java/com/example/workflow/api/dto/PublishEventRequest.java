package com.example.workflow.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record PublishEventRequest(
    @NotBlank String eventName,
    @NotBlank String correlationId,
    Map<String, Object> payload
) {}
