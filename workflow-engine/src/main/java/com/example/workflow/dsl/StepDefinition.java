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
    CompensationSpec compensation,
    /** When {@code type == SUB_WORKFLOW}: catalog id of the definition to run. */
    String subWorkflowDefinitionId,
    /** Optional static input merged on top of the child seed context. */
    Map<String, Object> subWorkflowInput,
    /**
     * When true, the child starts with only {@link #subWorkflowInput} (no copy of the parent context).
     * When null/false, non-internal parent entries are copied into the child before applying {@link #subWorkflowInput}.
     */
    Boolean subWorkflowIsolateContext,
    /**
     * When set, the child's output context (sanitized) is stored under this key on the parent.
     * When null, sanitized child outputs are merged at the root of the parent context.
     */
    String subWorkflowOutputKey
) {}
