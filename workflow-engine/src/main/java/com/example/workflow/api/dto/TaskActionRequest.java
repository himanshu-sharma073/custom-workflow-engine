package com.example.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Payload when approving or rejecting a task")
public record TaskActionRequest(
    @Schema(description = "Workflow id (validated against task)")
    String workflowId,
    @Schema(description = "Approval configuration key if applicable")
    String approvalType,
    @Schema(description = "Committee-style quorum override if applicable")
    Integer minApprovals,
    @Schema(description = "Free-form step input / decision map")
    Map<String, Object> input
) {}
