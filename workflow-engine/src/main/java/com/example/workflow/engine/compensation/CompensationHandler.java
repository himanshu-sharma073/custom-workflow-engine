package com.example.workflow.engine.compensation;

import com.example.workflow.dsl.StepDefinition;

import java.util.Map;

public interface CompensationHandler {
    void compensate(StepDefinition step, Map<String, Object> context);
}
