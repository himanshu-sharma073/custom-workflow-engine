package com.example.workflow.definition;

import com.example.workflow.dsl.WorkflowDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachingWorkflowDefinitionLoader implements WorkflowDefinitionLoader {
    private final WorkflowDefinitionLoader delegate;
    private final boolean cacheEnabled;
    private final Map<String, WorkflowDefinition> cache = new ConcurrentHashMap<>();

    public CachingWorkflowDefinitionLoader(WorkflowDefinitionLoader delegate, boolean cacheEnabled) {
        this.delegate = delegate;
        this.cacheEnabled = cacheEnabled;
    }

    @Override
    public WorkflowDefinition load(String workflowId) {
        if (!cacheEnabled) {
            return delegate.load(workflowId);
        }
        return cache.computeIfAbsent(workflowId, delegate::load);
    }
}
