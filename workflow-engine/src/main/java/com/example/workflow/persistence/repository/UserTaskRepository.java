package com.example.workflow.persistence.repository;

import com.example.workflow.persistence.entity.UserTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserTaskRepository extends JpaRepository<UserTaskEntity, UUID> {
    List<UserTaskEntity> findByStatus(String status);
}
