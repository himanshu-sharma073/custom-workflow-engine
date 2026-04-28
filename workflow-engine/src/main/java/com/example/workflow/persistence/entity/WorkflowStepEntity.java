package com.example.workflow.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_steps")
public class WorkflowStepEntity {
    @Id
    private UUID id;
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;
    @Column(name = "step_id", nullable = false)
    private String stepId;
    @Column(nullable = false)
    private String status;
    @Column(name = "context_snapshot", length = 4000)
    private String contextSnapshot;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInstanceId() { return instanceId; }
    public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }
    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getContextSnapshot() { return contextSnapshot; }
    public void setContextSnapshot(String contextSnapshot) { this.contextSnapshot = contextSnapshot; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
