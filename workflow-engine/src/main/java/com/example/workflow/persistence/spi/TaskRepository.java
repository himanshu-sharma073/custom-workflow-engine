package com.example.workflow.persistence.spi;

import com.example.workflow.persistence.entity.UserTaskEntity;

import java.util.List;
import java.util.UUID;

public interface TaskRepository {
    List<UserTaskEntity> findVisibleTasks();
    UserTaskEntity claim(UUID taskId);
}
