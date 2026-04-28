package com.example.workflow.api;

import com.example.workflow.api.dto.PublishEventRequest;
import com.example.workflow.api.dto.WorkflowEventResponse;
import com.example.workflow.engine.WorkflowExecutionService;
import com.example.workflow.persistence.repository.WorkflowAuditEventRepository;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ConditionalOnProperty(prefix = "workflow.engine.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${workflow.engine.api.base-path:/workflows}")
public class WorkflowRuntimeController {
    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowAuditEventRepository auditEventRepository;

    public WorkflowRuntimeController(WorkflowExecutionService workflowExecutionService,
                                     WorkflowAuditEventRepository auditEventRepository) {
        this.workflowExecutionService = workflowExecutionService;
        this.auditEventRepository = auditEventRepository;
    }

    @PostMapping("/{workflowId}/events")
    public String publishEvent(@PathVariable("workflowId") String workflowId, @Valid @RequestBody PublishEventRequest request) {
        boolean delivered = workflowExecutionService.publishEvent(workflowId, request.eventName(), request.correlationId(), request.payload());
        return delivered ? "event delivered" : "no workflow waiting for event";
    }

    @GetMapping("/{workflowId}/events")
    public List<WorkflowEventResponse> events(@PathVariable("workflowId") String workflowId) {
        return auditEventRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId).stream()
            .map(e -> new WorkflowEventResponse(e.getEventType(), e.getPayload(), e.getCreatedAt()))
            .toList();
    }
}
