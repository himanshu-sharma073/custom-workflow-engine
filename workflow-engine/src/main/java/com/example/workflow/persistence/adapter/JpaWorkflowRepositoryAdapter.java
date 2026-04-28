package com.example.workflow.persistence.adapter;

import com.example.workflow.persistence.entity.WorkflowInstanceEntity;
import com.example.workflow.persistence.repository.WorkflowInstanceJpaRepository;
import com.example.workflow.persistence.spi.WorkflowRepository;
import com.example.workflow.persistence.spi.WorkflowState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class JpaWorkflowRepositoryAdapter implements WorkflowRepository {
    private final WorkflowInstanceJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaWorkflowRepositoryAdapter(WorkflowInstanceJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public WorkflowState create(String definitionId, Map<String, Object> context) {
        WorkflowInstanceEntity entity = new WorkflowInstanceEntity();
        String workflowId = UUID.randomUUID().toString();
        entity.setId(UUID.randomUUID());
        entity.setWorkflowId(workflowId);
        entity.setDefinitionId(definitionId);
        entity.setStatus("RUNNING");
        entity.setContextJson(write(context));
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
        return toState(entity);
    }

    @Override
    public Optional<WorkflowState> find(String workflowId) {
        return repository.findByWorkflowId(workflowId).map(this::toState);
    }

    @Override
    public List<WorkflowState> findAll() {
        return repository.findAll().stream().map(this::toState).toList();
    }

    @Override
    public void updateState(String workflowId, String status, String currentStep, Map<String, Object> context) {
        WorkflowInstanceEntity entity = repository.findByWorkflowId(workflowId).orElseThrow();
        entity.setStatus(status);
        entity.setCurrentStep(currentStep);
        entity.setContextJson(write(context));
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
    }

    private WorkflowState toState(WorkflowInstanceEntity entity) {
        return new WorkflowState(entity.getWorkflowId(), entity.getDefinitionId(), entity.getStatus(), entity.getCurrentStep(), read(entity.getContextJson()), entity.getUpdatedAt());
    }

    private String write(Map<String, Object> context) {
        try {
            return objectMapper.writeValueAsString(context == null ? Map.of() : context);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> read(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
