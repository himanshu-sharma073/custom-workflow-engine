package com.example.workflow.api;

import com.example.workflow.api.dto.ApprovalHistoryResponse;
import com.example.workflow.api.dto.TaskActionRequest;
import com.example.workflow.api.dto.UserTaskResponse;
import com.example.workflow.engine.UserTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@ConditionalOnProperty(prefix = "workflow.engine.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${workflow.engine.api.base-path:/workflows}/tasks")
@Tag(name = "Tasks", description = "User tasks: list, claim, approve, reject, approval trail")
public class TaskController {
    private final UserTaskService userTaskService;

    public TaskController(UserTaskService userTaskService) {
        this.userTaskService = userTaskService;
    }

    @Operation(summary = "My tasks", description = "Tasks assigned to or claimable by the current user.")
    @ApiResponse(responseCode = "200", description = "Open tasks for this user")
    @GetMapping
    public List<UserTaskResponse> tasks() {
        return userTaskService.findMyTasks().stream()
            .map(t -> new UserTaskResponse(t.getId(), t.getWorkflowId(), t.getStepId(), t.getStatus(), t.getAssignmentType(), t.getAssignmentValue(), t.getClaimedBy(), t.getCreatedAt()))
            .toList();
    }

    @Operation(summary = "Claim task")
    @ApiResponse(responseCode = "200", description = "Task after claim")
    @PostMapping("/{taskId}/claim")
    public UserTaskResponse claim(
        @Parameter(description = "User task id", required = true)
        @PathVariable("taskId") UUID taskId) {
        var t = userTaskService.claim(taskId);
        return new UserTaskResponse(t.getId(), t.getWorkflowId(), t.getStepId(), t.getStatus(), t.getAssignmentType(), t.getAssignmentValue(), t.getClaimedBy(), t.getCreatedAt());
    }

    @Operation(summary = "Approve task", description = "Completes approval with form data / routing input.")
    @ApiResponse(responseCode = "200", description = "Updated task row")
    @PostMapping("/{taskId}/approve")
    public UserTaskResponse approve(
        @Parameter(description = "User task id", required = true)
        @PathVariable("taskId") UUID taskId,
        @Valid @RequestBody TaskActionRequest request) {
        var t = userTaskService.approve(taskId, request);
        return new UserTaskResponse(t.getId(), t.getWorkflowId(), t.getStepId(), t.getStatus(), t.getAssignmentType(), t.getAssignmentValue(), t.getClaimedBy(), t.getCreatedAt());
    }

    @Operation(summary = "Reject task")
    @ApiResponse(responseCode = "200", description = "Updated task row")
    @PostMapping("/{taskId}/reject")
    public UserTaskResponse reject(
        @Parameter(description = "User task id", required = true)
        @PathVariable("taskId") UUID taskId,
        @Valid @RequestBody TaskActionRequest request) {
        var t = userTaskService.reject(taskId, request);
        return new UserTaskResponse(t.getId(), t.getWorkflowId(), t.getStepId(), t.getStatus(), t.getAssignmentType(), t.getAssignmentValue(), t.getClaimedBy(), t.getCreatedAt());
    }

    @Operation(summary = "Approval history", description = "Decisions recorded for this task.")
    @ApiResponse(responseCode = "200", description = "Chronological approvals")
    @GetMapping("/{taskId}/approvals")
    public List<ApprovalHistoryResponse> approvals(
        @Parameter(description = "User task id", required = true)
        @PathVariable("taskId") UUID taskId) {
        return userTaskService.approvals(taskId).stream()
            .map(a -> new ApprovalHistoryResponse(a.getUserId(), a.getDecision(), a.getTimestamp()))
            .toList();
    }
}
