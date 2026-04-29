package com.example.workflow.engine;

import com.example.workflow.api.dto.TaskActionRequest;
import com.example.workflow.assignment.AssignmentEvaluator;
import com.example.workflow.auth.UserContextProvider;
import com.example.workflow.dsl.ApprovalType;
import com.example.workflow.dsl.AssignmentSpec;
import com.example.workflow.dsl.AssignmentType;
import com.example.workflow.dsl.CandidateSpec;
import com.example.workflow.persistence.entity.UserTaskApprovalEntity;
import com.example.workflow.persistence.entity.UserTaskCandidateEntity;
import com.example.workflow.persistence.entity.UserTaskEntity;
import com.example.workflow.persistence.entity.WorkflowAuditEventEntity;
import com.example.workflow.persistence.repository.UserTaskApprovalRepository;
import com.example.workflow.persistence.repository.UserTaskCandidateRepository;
import com.example.workflow.persistence.repository.UserTaskRepository;
import com.example.workflow.persistence.repository.WorkflowAuditEventRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class UserTaskService {
    private final UserTaskRepository taskRepository;
    private final UserTaskCandidateRepository candidateRepository;
    private final UserTaskApprovalRepository approvalRepository;
    private final WorkflowAuditEventRepository auditRepository;
    private final UserContextProvider userContextProvider;
    private final AssignmentEvaluator assignmentEvaluator;
    private final ApprovalPolicyService approvalPolicyService;
    private final WorkflowExecutionService workflowExecutionService;

    public UserTaskService(UserTaskRepository taskRepository,
                           UserTaskCandidateRepository candidateRepository,
                           UserTaskApprovalRepository approvalRepository,
                           WorkflowAuditEventRepository auditRepository,
                           UserContextProvider userContextProvider,
                           AssignmentEvaluator assignmentEvaluator,
                           ApprovalPolicyService approvalPolicyService,
                           @Lazy WorkflowExecutionService workflowExecutionService) {
        this.taskRepository = taskRepository;
        this.candidateRepository = candidateRepository;
        this.approvalRepository = approvalRepository;
        this.auditRepository = auditRepository;
        this.userContextProvider = userContextProvider;
        this.assignmentEvaluator = assignmentEvaluator;
        this.approvalPolicyService = approvalPolicyService;
        this.workflowExecutionService = workflowExecutionService;
    }

    @Transactional(readOnly = true)
    public List<UserTaskEntity> findMyTasks() {
        List<UserTaskEntity> pending = taskRepository.findByStatus("PENDING");
        Map<String, Object> context = buildContext();
        List<UserTaskEntity> visible = new ArrayList<>();
        for (UserTaskEntity task : pending) {
            List<UserTaskCandidateEntity> candidates = candidateRepository.findByTaskId(task.getId());
            List<CandidateSpec> candidateSpecs = candidates.stream()
                .map(c -> new CandidateSpec(AssignmentType.valueOf(c.getType()), c.getValue()))
                .toList();
            AssignmentSpec assignmentSpec = task.getAssignmentType() == null ? null :
                new AssignmentSpec(AssignmentType.valueOf(task.getAssignmentType()), task.getAssignmentValue());
            if (assignmentEvaluator.canView(assignmentSpec, candidateSpecs, context)) {
                visible.add(task);
            }
        }
        return visible;
    }

    @Transactional
    public UserTaskEntity claim(UUID taskId) {
        UserTaskEntity task = taskRepository.findById(taskId).orElseThrow();
        task.setClaimedBy(userContextProvider.getCurrentUser());
        audit(task, "CLAIM");
        return taskRepository.save(task);
    }

    @Transactional
    public UserTaskEntity approve(UUID taskId, TaskActionRequest request) {
        return recordDecision(taskId, request, "APPROVED");
    }

    @Transactional
    public UserTaskEntity reject(UUID taskId, TaskActionRequest request) {
        return recordDecision(taskId, request, "REJECTED");
    }

    @Transactional(readOnly = true)
    public List<UserTaskApprovalEntity> approvals(UUID taskId) {
        return approvalRepository.findByTaskId(taskId);
    }

    @Transactional
    public UserTaskEntity createUserTask(String workflowId,
                                         String stepId,
                                         AssignmentSpec assignment,
                                         List<CandidateSpec> candidates) {
        UserTaskEntity task = new UserTaskEntity();
        task.setId(UUID.randomUUID());
        task.setWorkflowId(workflowId);
        task.setStepId(stepId);
        task.setStatus("PENDING");
        if (assignment != null) {
            task.setAssignmentType(assignment.type().name());
            String assignmentValue = assignment.value();
            if ("${initiator}".equals(assignmentValue)) {
                assignmentValue = userContextProvider.getCurrentUser();
            }
            task.setAssignmentValue(assignmentValue);
        }
        taskRepository.save(task);

        for (CandidateSpec c : candidates) {
            UserTaskCandidateEntity entity = new UserTaskCandidateEntity();
            entity.setTaskId(task.getId());
            entity.setType(c.type().name());
            entity.setValue(c.value());
            candidateRepository.save(entity);
        }
        audit(task, "CREATED");
        return task;
    }

    @Transactional
    public UserTaskEntity reopenTask(UUID taskId, boolean resetApprovals) {
        UserTaskEntity task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus("PENDING");
        task.setClaimedBy(null);
        if (resetApprovals) {
            approvalRepository.deleteByTaskId(taskId);
        }
        audit(task, "ROLLED_BACK_REOPEN");
        return taskRepository.save(task);
    }

    private UserTaskEntity recordDecision(UUID taskId, TaskActionRequest request, String decision) {
        UserTaskEntity task = taskRepository.findById(taskId).orElseThrow();
        if (!"PENDING".equals(task.getStatus())) {
            return task;
        }

        String user = userContextProvider.getCurrentUser();
        boolean hasExistingDecision = approvalRepository.existsByTaskIdAndUserId(taskId, user);
        if (!hasExistingDecision) {
            UserTaskApprovalEntity approval = new UserTaskApprovalEntity();
            approval.setTaskId(taskId);
            approval.setUserId(user);
            approval.setDecision(decision);
            approval.setTimestamp(Instant.now());
            approvalRepository.save(approval);
        }

        if ("REJECTED".equals(decision)) {
            task.setStatus("REJECTED");
            audit(task, "REJECT");
            UserTaskEntity saved = taskRepository.save(task);
            if (!hasExistingDecision) {
                Map<String, Object> ctx = new HashMap<>(request.input() == null ? Map.of() : request.input());
                ctx.put("approved", false);
                workflowExecutionService.onUserTaskCompleted(saved.getWorkflowId(), saved.getStepId(), ctx);
            }
            return saved;
        }

        ApprovalType approvalType = request.approvalType() == null ? ApprovalType.ANY : ApprovalType.valueOf(request.approvalType());
        boolean complete = approvalPolicyService.isCompleted(
            approvalType,
            request.minApprovals(),
            approvalRepository.findByTaskId(taskId),
            candidateRepository.findByTaskId(taskId)
        );
        if (complete) {
            task.setStatus("COMPLETED");
        }
        audit(task, "APPROVE");
        UserTaskEntity saved = taskRepository.save(task);
        if (complete && !hasExistingDecision) {
            Map<String, Object> ctx = new HashMap<>(request.input() == null ? Map.of() : request.input());
            ctx.put("approved", true);
            workflowExecutionService.onUserTaskCompleted(saved.getWorkflowId(), saved.getStepId(), ctx);
        }
        return saved;
    }

    private Map<String, Object> buildContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("currentUser", userContextProvider.getCurrentUser());
        context.put("roles", userContextProvider.getUserRoles());
        context.put("groups", userContextProvider.getUserGroups());
        return context;
    }

    private void audit(UserTaskEntity task, String eventType) {
        WorkflowAuditEventEntity event = new WorkflowAuditEventEntity();
        event.setId(UUID.randomUUID());
        event.setWorkflowId(task.getWorkflowId());
        event.setTaskId(task.getId());
        event.setEventType(eventType);
        event.setPayload("{}");
        event.setCreatedAt(Instant.now());
        auditRepository.save(event);
    }
}
