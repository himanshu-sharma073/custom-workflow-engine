package com.example.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current authenticated user as seen by the workflow user context")
public record SessionUserResponse(
    @Schema(description = "Principal name / user id", example = "user123")
    String userName
) {}
