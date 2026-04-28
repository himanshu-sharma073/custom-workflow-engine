package com.example.workflow.engine.handler;

public record StepExecutionResult(String nextStepId, boolean waiting, boolean ended) {
    public static StepExecutionResult next(String nextStep) { return new StepExecutionResult(nextStep, false, false); }
    public static StepExecutionResult waitState() { return new StepExecutionResult(null, true, false); }
    public static StepExecutionResult endState() { return new StepExecutionResult(null, false, true); }
}
