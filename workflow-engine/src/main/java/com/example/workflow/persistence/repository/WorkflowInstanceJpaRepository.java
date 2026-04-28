package com.example.workflow.persistence.repository;

import com.example.workflow.persistence.entity.WorkflowInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowInstanceJpaRepository extends JpaRepository<WorkflowInstanceEntity, UUID> {
    Optional<WorkflowInstanceEntity> findByWorkflowId(String workflowId);
}
