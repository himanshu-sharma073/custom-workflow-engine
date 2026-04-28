package com.example.workflow.api.dto;

import java.time.Instant;

public record ApprovalHistoryResponse(String userId, String decision, Instant timestamp) {}
