package com.example.workflow.api.dto;

import java.time.Instant;
import java.util.UUID;

public record UserTaskResponse(UUID id, String workflowId, String stepId, String status, String assignmentType, String assignmentValue, String claimedBy, Instant createdAt) {}
