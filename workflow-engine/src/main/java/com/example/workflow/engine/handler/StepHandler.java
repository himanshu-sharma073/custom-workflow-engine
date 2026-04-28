package com.example.workflow.engine.handler;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;

public interface StepHandler {
    StepType supports();
    StepExecutionResult execute(StepDefinition step, StepExecutionContext context);
}
