package com.example.workflow.engine.listener;

import com.example.workflow.dsl.StepDefinition;

import java.util.Map;

public interface StepExecutionListener {
    default void beforeStep(String workflowId, StepDefinition step, Map<String, Object> context) {}
    default void afterStep(String workflowId, StepDefinition step, Map<String, Object> context) {}
}
