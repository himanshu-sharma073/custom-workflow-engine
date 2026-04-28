package com.example.workflow.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_task_candidates")
public class UserTaskCandidateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "task_id", nullable = false)
    private java.util.UUID taskId;
    @Column(nullable = false)
    private String type;
    @Column(name = "candidate_value", nullable = false)
    private String value;

    public Long getId() { return id; }
    public java.util.UUID getTaskId() { return taskId; }
    public void setTaskId(java.util.UUID taskId) { this.taskId = taskId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
