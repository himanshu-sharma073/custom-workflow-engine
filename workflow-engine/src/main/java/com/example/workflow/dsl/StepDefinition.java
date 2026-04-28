package com.example.workflow.dsl;

import java.util.List;
import java.util.Map;

public record StepDefinition(
    String id,
    StepType type,
    AssignmentSpec assignment,
    List<CandidateSpec> candidates,
    ApprovalSpec approval,
    String next,
    String action,
    List<DecisionCondition> conditions,
    String url,
    String method,
    Map<String, String> headers,
    Object body,
    Integer retryAttempts,
    Map<String, String> responseMapping,
    String eventName,
    String correlationId,
    Long delayMs,
    String script,
    CompensationSpec compensation
) {}
