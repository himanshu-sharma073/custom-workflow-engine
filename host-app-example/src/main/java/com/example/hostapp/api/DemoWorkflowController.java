package com.example.hostapp.api;

import com.example.workflow.engine.WorkflowEngine;
import com.example.workflow.persistence.spi.WorkflowState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/demo/workflows")
@Tag(name = "Demo", description = "Sample helpers for trying the engine (host app)")
public class DemoWorkflowController {
    private final WorkflowEngine workflowEngine;

    public DemoWorkflowController(WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    @Operation(summary = "Start sample ratings workflow", description = "Starts `ratings-review-workflow` with a fixed initiator for demos.")
    @ApiResponse(responseCode = "200", description = "Initial workflow state")
    @PostMapping("/start-sample")
    public WorkflowState startSampleWorkflow() {
        return workflowEngine.startWorkflow("ratings-review-workflow", Map.of("initiator", "user123"));
    }

    @Operation(summary = "Rollback sample workflow", description = "Calls rollback with no target step (engine default).")
    @ApiResponse(responseCode = "200", description = "State after rollback")
    @PostMapping("/rollback/{workflowId}")
    public WorkflowState rollback(
        @Parameter(description = "Running workflow id", required = true)
        @PathVariable("workflowId") String workflowId) {
        return workflowEngine.rollbackWorkflow(workflowId, null);
    }
}
