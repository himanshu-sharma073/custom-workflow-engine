package com.example.hostapp.api;

import com.example.workflow.engine.WorkflowEngine;
import com.example.workflow.persistence.spi.WorkflowState;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/demo/workflows")
public class DemoWorkflowController {
    private final WorkflowEngine workflowEngine;

    public DemoWorkflowController(WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    @PostMapping("/start-sample")
    public WorkflowState startSampleWorkflow() {
        return workflowEngine.startWorkflow("ratings-review-workflow", Map.of("initiator", "user123"));
    }

    @PostMapping("/rollback/{workflowId}")
    public WorkflowState rollback(@PathVariable("workflowId") String workflowId) {
        return workflowEngine.rollbackWorkflow(workflowId, null);
    }
}
