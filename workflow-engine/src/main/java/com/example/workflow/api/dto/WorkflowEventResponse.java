package com.example.workflow.api.dto;

import java.time.Instant;

public record WorkflowEventResponse(String eventType, String payload, Instant createdAt) {}
