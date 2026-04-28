package com.example.workflow.engine.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowRuntimeState {
    private final Map<String, Object> context = new ConcurrentHashMap<>();

    public Map<String, Object> context() {
        return context;
    }
}
