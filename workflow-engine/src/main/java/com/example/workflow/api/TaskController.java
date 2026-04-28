package com.example.workflow.api;

import com.example.workflow.api.dto.ApprovalHistoryResponse;
import com.example.workflow.api.dto.TaskActionRequest;
import com.example.workflow.api.dto.UserTaskResponse;
import com.example.workflow.engine.UserTaskService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@ConditionalOnProperty(prefix = "workflow.engine.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${workflow.engine.api.base-path:/workflows}/tasks")
public class TaskController {
    private final UserTaskService userTaskService;

    public TaskController(UserTaskService userTaskService) {
        this.userTaskService = userTaskService;
    }

    @GetMapping
    public List<UserTaskResponse> tasks() {
        return userTaskService.findMyTasks().stream()
            .map(t -> new UserTaskResponse(t.getId(), t.getWorkflowId(), t.getStepId(), t.getStatus(), t.getAssignmentType(), t.getAssignmentValue(), t.getClaimedBy(), t.getCreatedAt()))
            .toList();
    }

    @PostMapping("/{taskId}/claim")
    public UserTaskResponse claim(@PathVariable("taskId") UUID taskId) {
        var t = userTaskService.claim(taskId);
        return new UserTaskResponse(t.getId(), t.getWorkflowId(), t.getStepId(), t.getStatus(), t.getAssignmentType(), t.getAssignmentValue(), t.getClaimedBy(), t.getCreatedAt());
    }

    @PostMapping("/{taskId}/approve")
    public UserTaskResponse approve(@PathVariable("taskId") UUID taskId, @Valid @RequestBody TaskActionRequest request) {
        var t = userTaskService.approve(taskId, request);
        return new UserTaskResponse(t.getId(), t.getWorkflowId(), t.getStepId(), t.getStatus(), t.getAssignmentType(), t.getAssignmentValue(), t.getClaimedBy(), t.getCreatedAt());
    }

    @PostMapping("/{taskId}/reject")
    public UserTaskResponse reject(@PathVariable("taskId") UUID taskId, @Valid @RequestBody TaskActionRequest request) {
        var t = userTaskService.reject(taskId, request);
        return new UserTaskResponse(t.getId(), t.getWorkflowId(), t.getStepId(), t.getStatus(), t.getAssignmentType(), t.getAssignmentValue(), t.getClaimedBy(), t.getCreatedAt());
    }

    @GetMapping("/{taskId}/approvals")
    public List<ApprovalHistoryResponse> approvals(@PathVariable("taskId") UUID taskId) {
        return userTaskService.approvals(taskId).stream()
            .map(a -> new ApprovalHistoryResponse(a.getUserId(), a.getDecision(), a.getTimestamp()))
            .toList();
    }
}
