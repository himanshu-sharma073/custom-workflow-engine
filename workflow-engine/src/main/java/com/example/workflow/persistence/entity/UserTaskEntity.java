package com.example.workflow.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_tasks")
public class UserTaskEntity {
    @Id
    private UUID id;
    @Column(name = "workflow_id", nullable = false)
    private String workflowId;
    @Column(name = "step_id", nullable = false)
    private String stepId;
    @Column(nullable = false)
    private String status;
    @Column(name = "assignment_type")
    private String assignmentType;
    @Column(name = "assignment_value")
    private String assignmentValue;
    @Column(name = "claimed_by")
    private String claimedBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAssignmentType() { return assignmentType; }
    public void setAssignmentType(String assignmentType) { this.assignmentType = assignmentType; }
    public String getAssignmentValue() { return assignmentValue; }
    public void setAssignmentValue(String assignmentValue) { this.assignmentValue = assignmentValue; }
    public String getClaimedBy() { return claimedBy; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
