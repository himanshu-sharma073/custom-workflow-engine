package com.example.workflow.persistence.adapter;

import com.example.workflow.persistence.entity.WorkflowStepEntity;
import com.example.workflow.persistence.repository.WorkflowInstanceJpaRepository;
import com.example.workflow.persistence.repository.WorkflowStepJpaRepository;
import com.example.workflow.persistence.spi.HistoryRepository;
import com.example.workflow.persistence.spi.WorkflowHistoryRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JpaHistoryRepositoryAdapter implements HistoryRepository {
    private final WorkflowStepJpaRepository stepRepository;
    private final WorkflowInstanceJpaRepository workflowInstanceRepository;
    private final ObjectMapper objectMapper;

    public JpaHistoryRepositoryAdapter(WorkflowStepJpaRepository stepRepository,
                                       WorkflowInstanceJpaRepository workflowInstanceRepository,
                                       ObjectMapper objectMapper) {
        this.stepRepository = stepRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(String workflowId, String stepId, String status, Map<String, Object> contextSnapshot) {
        var instance = workflowInstanceRepository.findByWorkflowId(workflowId).orElseThrow();
        WorkflowStepEntity step = new WorkflowStepEntity();
        step.setId(UUID.randomUUID());
        step.setInstanceId(instance.getId());
        step.setStepId(stepId);
        step.setStatus(status);
        step.setContextSnapshot(write(contextSnapshot));
        step.setCreatedAt(Instant.now());
        step.setUpdatedAt(Instant.now());
        stepRepository.save(step);
    }

    @Override
    public void markRolledBack(String workflowId, String stepId) {
        var instance = workflowInstanceRepository.findByWorkflowId(workflowId).orElseThrow();
        List<WorkflowStepEntity> steps = stepRepository.findByInstanceIdOrderByCreatedAtAsc(instance.getId());
        for (int i = steps.size() - 1; i >= 0; i--) {
            WorkflowStepEntity step = steps.get(i);
            if (step.getStepId().equals(stepId)) {
                step.setStatus("ROLLED_BACK");
                step.setUpdatedAt(Instant.now());
                stepRepository.save(step);
                return;
            }
        }
    }

    @Override
    public List<WorkflowHistoryRecord> history(String workflowId) {
        var instance = workflowInstanceRepository.findByWorkflowId(workflowId).orElseThrow();
        return stepRepository.findByInstanceIdOrderByCreatedAtAsc(instance.getId()).stream()
            .map(s -> new WorkflowHistoryRecord(workflowId, s.getStepId(), s.getStatus(), s.getContextSnapshot(), s.getCreatedAt()))
            .toList();
    }

    private String write(Map<String, Object> context) {
        try {
            return objectMapper.writeValueAsString(context == null ? Map.of() : context);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
