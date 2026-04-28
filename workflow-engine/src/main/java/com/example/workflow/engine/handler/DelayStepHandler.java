package com.example.workflow.engine.handler;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import org.springframework.stereotype.Component;

@Component
public class DelayStepHandler implements StepHandler {
    @Override
    public StepType supports() { return StepType.DELAY; }

    @Override
    public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
        long delay = step.delayMs() == null ? 1000L : step.delayMs();
        context.delayRegistrar().accept(delay, step.next());
        context.appendHistory(step, "DELAY_WAITING");
        return StepExecutionResult.waitState();
    }
}
