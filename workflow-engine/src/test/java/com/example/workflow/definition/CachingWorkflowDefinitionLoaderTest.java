package com.example.workflow.definition;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import com.example.workflow.dsl.WorkflowDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class CachingWorkflowDefinitionLoaderTest {

    @Test
    void cachesWhenEnabled() {
        AtomicInteger counter = new AtomicInteger();
        WorkflowDefinitionLoader delegate = id -> {
            counter.incrementAndGet();
            return new WorkflowDefinition(id, 1, List.of(new StepDefinition("s1", StepType.END, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null)));
        };

        WorkflowDefinitionLoader loader = new CachingWorkflowDefinitionLoader(delegate, true);
        loader.load("wf1");
        loader.load("wf1");

        Assertions.assertEquals(1, counter.get());
    }
}
