package com.example.workflow.engine.handler;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.persistence.spi.HistoryRepository;

import java.util.Map;
import java.util.function.BiConsumer;

public record StepExecutionContext(
    String workflowId,
    Map<String, Object> variables,
    HistoryRepository historyRepository,
    BiConsumer<String, Map<String, Object>> waitEventRegistrar,
    BiConsumer<Long, String> delayRegistrar
) {
    public void appendHistory(StepDefinition step, String status) {
        historyRepository.append(workflowId, step.id(), status, variables);
    }
}
