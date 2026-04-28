package com.example.workflow.persistence.repository;

import com.example.workflow.persistence.entity.UserTaskCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserTaskCandidateRepository extends JpaRepository<UserTaskCandidateEntity, Long> {
    List<UserTaskCandidateEntity> findByTaskId(UUID taskId);
}
