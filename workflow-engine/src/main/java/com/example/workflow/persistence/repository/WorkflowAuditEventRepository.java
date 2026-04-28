package com.example.workflow.persistence.repository;

import com.example.workflow.persistence.entity.WorkflowAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowAuditEventRepository extends JpaRepository<WorkflowAuditEventEntity, UUID> {
    List<WorkflowAuditEventEntity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
    List<WorkflowAuditEventEntity> findByWorkflowIdOrderByCreatedAtAsc(String workflowId);
}
