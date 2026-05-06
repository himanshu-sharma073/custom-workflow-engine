package com.example.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Single approval/reject decision in history")
public record ApprovalHistoryResponse(
    @Schema(example = "reviewer1")
    String userId,
    @Schema(example = "APPROVED")
    String decision,
    Instant timestamp
) {}
