package com.example.workflow.dsl;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Workflow definition document (JSON DSL)")
public record WorkflowDefinition(
    @Schema(example = "my-workflow")
    String id,
    Integer version,
    List<StepDefinition> steps
) {}
