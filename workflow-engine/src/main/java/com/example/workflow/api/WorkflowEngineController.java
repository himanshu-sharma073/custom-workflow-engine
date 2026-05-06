package com.example.workflow.api;

import com.example.workflow.api.dto.SessionUserResponse;
import com.example.workflow.auth.UserContextProvider;
import com.example.workflow.persistence.spi.WorkflowHistoryRecord;
import com.example.workflow.persistence.spi.WorkflowState;
import com.example.workflow.engine.WorkflowEngine;
import com.example.workflow.engine.WorkflowExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@ConditionalOnProperty(prefix = "workflow.engine.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${workflow.engine.api.base-path:/workflows}")
@Tag(name = "Workflow engine", description = "Start, resume, query, and roll back workflow instances")
public class WorkflowEngineController {
    private final WorkflowEngine workflowEngine;
    private final WorkflowExecutionService workflowExecutionService;
    private final UserContextProvider userContextProvider;

    public WorkflowEngineController(WorkflowEngine workflowEngine,
                                    WorkflowExecutionService workflowExecutionService,
                                    UserContextProvider userContextProvider) {
        this.workflowEngine = workflowEngine;
        this.workflowExecutionService = workflowExecutionService;
        this.userContextProvider = userContextProvider;
    }

    @Operation(summary = "Current user context", description = "User id resolved from security / host integration (for assignments and tasks).")
    @ApiResponse(responseCode = "200", description = "Wrapped user name from context provider")
    @GetMapping("/session/me")
    public SessionUserResponse currentUser() {
        return new SessionUserResponse(userContextProvider.getCurrentUser());
    }

    @Operation(summary = "Start workflow", description = "Creates a new instance from a definition id with optional input map.")
    @ApiResponse(responseCode = "200", description = "New workflow state")
    @PostMapping("/start")
    public WorkflowState start(
        @Parameter(description = "Catalog definition id", required = true, example = "ratings-review-workflow")
        @RequestParam("definitionId") String definitionId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional variables merged into workflow context",
            content = @Content(examples = @ExampleObject(value = "{\"initiator\": \"user123\", \"amount\": 100}")))
        @RequestBody(required = false) Map<String, Object> input) {
        return workflowEngine.startWorkflow(definitionId, input == null ? Map.of() : input);
    }

    @Operation(summary = "Resume workflow", description = "Continues execution with optional payload for the next transition.")
    @ApiResponse(responseCode = "200", description = "Updated workflow state")
    @PostMapping("/{id}/resume")
    public WorkflowState resume(
        @Parameter(description = "Workflow instance id", required = true)
        @PathVariable("id") String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional decision/payload for the active step",
            content = @Content(examples = @ExampleObject(value = "{\"approved\": true}")))
        @RequestBody(required = false) Map<String, Object> input) {
        return workflowEngine.resumeWorkflow(id, input == null ? Map.of() : input);
    }

    @Operation(summary = "Rollback workflow", description = "Moves the instance back to a prior step (full rollback or to target step if supported).")
    @ApiResponse(responseCode = "200", description = "State after rollback")
    @PostMapping("/{id}/rollback")
    public WorkflowState rollback(
        @Parameter(description = "Workflow instance id", required = true)
        @PathVariable("id") String id,
        @Parameter(description = "Step id to roll back to; omit for engine default")
        @RequestParam(value = "targetStepId", required = false) String targetStepId) {
        return workflowEngine.rollbackWorkflow(id, targetStepId);
    }

    @Operation(summary = "Get workflow by id")
    @ApiResponse(responseCode = "200", description = "Current snapshot")
    @GetMapping("/{id}")
    public WorkflowState get(
        @Parameter(description = "Workflow instance id", required = true)
        @PathVariable("id") String id) {
        return workflowEngine.getWorkflow(id);
    }

    @Operation(summary = "List workflows", description = "Returns persisted running/completed instances visible to the engine.")
    @ApiResponse(responseCode = "200", description = "Workflow states")
    @GetMapping
    public List<WorkflowState> list() {
        return workflowExecutionService.listWorkflows();
    }

    @Operation(summary = "Workflow audit history", description = "Step-level history records for the instance.")
    @ApiResponse(responseCode = "200", description = "Ordered history entries")
    @GetMapping("/{id}/history")
    public List<WorkflowHistoryRecord> history(
        @Parameter(description = "Workflow instance id", required = true)
        @PathVariable("id") String id) {
        return workflowExecutionService.history(id);
    }
}
