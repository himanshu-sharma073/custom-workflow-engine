package com.example.workflow.persistence.repository;

import com.example.workflow.persistence.entity.UserTaskApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserTaskApprovalRepository extends JpaRepository<UserTaskApprovalEntity, Long> {
    List<UserTaskApprovalEntity> findByTaskId(UUID taskId);
    long countByTaskIdAndDecision(UUID taskId, String decision);
    boolean existsByTaskIdAndUserId(UUID taskId, String userId);
    void deleteByTaskId(UUID taskId);
}
