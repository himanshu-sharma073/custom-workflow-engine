package com.example.workflow.engine.handler;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import org.springframework.stereotype.Component;

@Component
public class EventStepHandler implements StepHandler {
    @Override
    public StepType supports() { return StepType.EVENT; }

    @Override
    public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
        String correlation = step.correlationId();
        if (correlation != null) {
            for (var e : context.variables().entrySet()) {
                correlation = correlation.replace("${" + e.getKey() + "}", String.valueOf(e.getValue()));
            }
        }
        context.waitEventRegistrar().accept(step.eventName() + ":" + correlation, context.variables());
        context.appendHistory(step, "EVENT_WAITING");
        return StepExecutionResult.waitState();
    }
}
