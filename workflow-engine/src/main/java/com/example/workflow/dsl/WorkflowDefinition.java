package com.example.workflow.dsl;

import java.util.List;

public record WorkflowDefinition(String id, Integer version, List<StepDefinition> steps) {}
