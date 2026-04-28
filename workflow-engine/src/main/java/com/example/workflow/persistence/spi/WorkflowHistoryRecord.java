package com.example.workflow.persistence.spi;

import java.time.Instant;

public record WorkflowHistoryRecord(String workflowId, String stepId, String status, String payload, Instant createdAt) {}
