package com.example.workflow.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_task_approvals", uniqueConstraints = {
    @UniqueConstraint(name = "uk_task_user", columnNames = {"task_id", "user_id"})
})
public class UserTaskApprovalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "task_id", nullable = false)
    private java.util.UUID taskId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(nullable = false)
    private String decision;
    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    public Long getId() { return id; }
    public java.util.UUID getTaskId() { return taskId; }
    public void setTaskId(java.util.UUID taskId) { this.taskId = taskId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
