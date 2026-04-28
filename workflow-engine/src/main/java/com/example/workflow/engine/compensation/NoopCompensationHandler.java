package com.example.workflow.engine.compensation;

import com.example.workflow.dsl.StepDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnMissingBean(CompensationHandler.class)
public class NoopCompensationHandler implements CompensationHandler {
    @Override
    public void compensate(StepDefinition step, Map<String, Object> context) {
        // Host applications can provide a bean to run actual compensation logic.
    }
}
