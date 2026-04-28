package com.example.workflow.api;

import com.example.workflow.api.dto.TaskActionRequest;
import com.example.workflow.persistence.spi.WorkflowState;
import com.example.workflow.persistence.spi.WorkflowHistoryRecord;
import com.example.workflow.engine.WorkflowEngine;
import com.example.workflow.engine.WorkflowExecutionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@ConditionalOnProperty(prefix = "workflow.engine.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${workflow.engine.api.base-path:/workflows}")
public class WorkflowEngineController {
    private final WorkflowEngine workflowEngine;
    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowEngineController(WorkflowEngine workflowEngine, WorkflowExecutionService workflowExecutionService) {
        this.workflowEngine = workflowEngine;
        this.workflowExecutionService = workflowExecutionService;
    }

    @PostMapping("/start")
    public WorkflowState start(@RequestParam("definitionId") String definitionId, @RequestBody(required = false) Map<String, Object> input) {
        return workflowEngine.startWorkflow(definitionId, input == null ? Map.of() : input);
    }

    @PostMapping("/{id}/resume")
    public WorkflowState resume(@PathVariable("id") String id, @RequestBody(required = false) Map<String, Object> input) {
        return workflowEngine.resumeWorkflow(id, input == null ? Map.of() : input);
    }

    @PostMapping("/{id}/rollback")
    public WorkflowState rollback(@PathVariable("id") String id) {
        return workflowEngine.rollbackWorkflow(id);
    }

    @GetMapping("/{id}")
    public WorkflowState get(@PathVariable("id") String id) {
        return workflowEngine.getWorkflow(id);
    }

    @GetMapping
    public java.util.List<WorkflowState> list() {
        return workflowExecutionService.listWorkflows();
    }

    @GetMapping("/{id}/history")
    public java.util.List<WorkflowHistoryRecord> history(@PathVariable("id") String id) {
        return workflowExecutionService.history(id);
    }
}
