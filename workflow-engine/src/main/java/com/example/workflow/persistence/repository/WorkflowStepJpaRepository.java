package com.example.workflow.persistence.repository;

import com.example.workflow.persistence.entity.WorkflowStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowStepJpaRepository extends JpaRepository<WorkflowStepEntity, UUID> {
    List<WorkflowStepEntity> findByInstanceIdOrderByCreatedAtAsc(UUID instanceId);
}
