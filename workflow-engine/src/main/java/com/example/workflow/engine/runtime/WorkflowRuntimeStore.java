package com.example.workflow.engine.runtime;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorkflowRuntimeStore {
    private final Map<String, WorkflowRuntimeState> states = new ConcurrentHashMap<>();

    public WorkflowRuntimeState getOrCreate(String workflowId) {
        return states.computeIfAbsent(workflowId, key -> new WorkflowRuntimeState());
    }
}
