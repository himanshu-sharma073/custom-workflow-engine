package com.example.workflow.api.dto;

import java.util.Map;

public record TaskActionRequest(String workflowId, String approvalType, Integer minApprovals, Map<String, Object> input) {}
