package com.example.workflow.engine.listener;

import com.example.workflow.dsl.StepDefinition;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnBean(MeterRegistry.class)
public class StepMetricsListener implements StepExecutionListener {
    private final MeterRegistry meterRegistry;

    public StepMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void beforeStep(String workflowId, StepDefinition step, Map<String, Object> context) {
        meterRegistry.counter("workflow.step.started", "type", step.type().name()).increment();
    }

    @Override
    public void afterStep(String workflowId, StepDefinition step, Map<String, Object> context) {
        meterRegistry.counter("workflow.step.completed", "type", step.type().name()).increment();
    }
}
