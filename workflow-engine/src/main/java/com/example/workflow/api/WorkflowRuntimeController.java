package com.example.workflow.api;

import com.example.workflow.api.dto.PublishEventRequest;
import com.example.workflow.api.dto.WorkflowEventResponse;
import com.example.workflow.engine.WorkflowExecutionService;
import com.example.workflow.persistence.repository.WorkflowAuditEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ConditionalOnProperty(prefix = "workflow.engine.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${workflow.engine.api.base-path:/workflows}")
@Tag(name = "Workflow runtime", description = "Publish and inspect correlation events for running workflows")
public class WorkflowRuntimeController {
    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowAuditEventRepository auditEventRepository;

    public WorkflowRuntimeController(WorkflowExecutionService workflowExecutionService,
                                     WorkflowAuditEventRepository auditEventRepository) {
        this.workflowExecutionService = workflowExecutionService;
        this.auditEventRepository = auditEventRepository;
    }

    @Operation(summary = "Publish workflow event", description = """
        Delivers an external event to a waiting workflow step (e.g. API or event correlation). \
        Returns whether a running instance consumed the event.""")
    @ApiResponse(responseCode = "200", description = "Plain-text outcome",
        content = @Content(schema = @Schema(type = "string", example = "event delivered")))
    @PostMapping("/{workflowId}/events")
    public String publishEvent(
        @Parameter(description = "Running workflow instance id", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable("workflowId") String workflowId,
        @Valid @RequestBody PublishEventRequest request) {
        boolean delivered = workflowExecutionService.publishEvent(workflowId, request.eventName(), request.correlationId(), request.payload());
        return delivered ? "event delivered" : "no workflow waiting for event";
    }

    @Operation(summary = "List audit events", description = "Returns persisted events for the workflow, oldest first.")
    @ApiResponse(responseCode = "200", description = "Event audit trail")
    @GetMapping("/{workflowId}/events")
    public List<WorkflowEventResponse> events(
        @Parameter(description = "Workflow instance id", required = true)
        @PathVariable("workflowId") String workflowId) {
        return auditEventRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId).stream()
            .map(e -> new WorkflowEventResponse(e.getEventType(), e.getPayload(), e.getCreatedAt()))
            .toList();
    }
}
